# dtbTerminalIO — Architecture Notes

Working notes on how the library actually works, warts included. Written from
reading the code (Sep 2026), not from original design intent — where intent is
guessed, it says so. Scope so far: **event system and focus**. Other areas
(rendering, layout, plotting) are not yet covered.

---

## 1. Event system

### 1.1 The bus — `events/EventBusImpl`

- One bounded queue (`ArrayBlockingQueue`, 512) and **one consumer thread**
  (`EventBus-Thread-N`, started in the constructor). All handlers run on that
  thread, so handler code is effectively single-threaded.
- Producers call `raiseEvent(te)` from any thread (keyboard reader, timers, UI code).
- Each dequeued event gets `te.setEventBus(bus)` before dispatch and
  `setEventBus(null)` after — handlers reach the bus via `te.getEventBus()`.
- Exceptions thrown by handlers are caught and re-dispatched as an
  `EXCEPTION` event; the loop keeps running.

### 1.2 Handler contract

```java
interface EventHandler            { void handle(TerminalEvent<?> te); }
interface ConditionalEventHandler { EventHandler canHandle(TerminalEvent<?> te); } // null = "not mine"
```

`canHandle` returns the handler that *will* process the event (usually `this`),
or `null`. It is meant to be a query, but in practice several `canHandle`
implementations **change state** (see §4).

### 1.3 Chains — `events/ChainedEventHandler`

A chain is a list of `ConditionalEventHandler`s with one of two strategies:

| Strategy | Behaviour | Used for |
|---|---|---|
| Chain-of-responsibility | first handler whose `canHandle` ≠ null gets the event, nobody else | main bus chain, keyboard, focus managers |
| Handle-all | every handler whose `canHandle` ≠ null gets the event | idle (TIMER) and TERMINATE chains |

Chains nest: `asConditionalEventHandler()` wraps a chain so it can sit inside
another chain.

**Deferred mutation:** `append/prepend/removeEventHandler` only queue the change;
it is applied at the start and end of the next `handle()` call. A handler added
during dispatch does not see the current event.

### 1.4 Bus layout

```
EventBusImpl.queue ──► main chain (chain-of-responsibility)
                          ├─ master_idlEventHandlerBRIDGE ──► idle chain (handle-all)   [TIMER]
                          ├─ master_terminateEventHandlerBRIDGE ──► terminate chain     [TERMINATE]
                          ├─ root FocusManager (added by TUIApp)                         [KEYPRESS, REQUEST_FOCUS, ...]
                          ├─ MouseEventOrchestrator handlers                             [RAW_MOUSE → ApplicationMouseEvent]
                          └─ ... app handlers (KeyInputEventHandler, etc.)
```

Order matters: the first handler that claims an event wins.

---

## 2. Focus model

### 2.1 Types

| Type | Role |
|---|---|
| `FocusBehaviour` | focus *state*: `hasFocus`, `setFocus`, `focus/unFocus` hooks; extends `Enabled` |
| `Focusable` | `FocusBehaviour` + `ConditionalEventHandler` + a parent `FocusManager` pointer |
| `ToFocusable` | "I am not a Focusable, but here is one for me" (`asFocusable()`) |
| `FocusManager` | a `Focusable` that owns child focusables and one *current child* |
| `DefaultFocusManager` | the only real `FocusManager` implementation; everything else delegates to one |

A `FocusManager` is itself a `Focusable`, so managers nest into a **focus tree**.
Focus state is **distributed**: each manager holds its own `current` child and
its own `hasFocus` flag. The focused leaf is found by walking `current` pointers
from the root (`FocusManager.UTIL.findCurrentFocus`).

### 2.2 Keyboard navigation and routing

**Keys** are delivered by `KeyRouter`, run by a *navigation root*: the application's root
`DefaultFocusManager` (no parent), or a modal dialog's (policy TRAP - it gets keys before the
application's root). A key goes to the focused component first, then up through the components
around it until one uses it ("bubbling", as in a web browser). At each focus manager on the way up:
its component's keyboard handler, a hot key of one of its other children, the manager's custom
handler (`setCustomExtendedEventHandler`), then its nav keys (`advance`, §4.3 A2-A3). Components
between two focus managers that have a keyboard handler are asked on the way up too.
A component's keyboard handler: the component itself if it implements `KeyboardHandler` (`onKey`),
else one it hands out (`AsKeyboardHandler`) - e.g. a `ComponentWithFocus`'s keys (`focus().handleKeys(..)`).
A focused leaf's own `Focusable` handler is also offered the key, first.
Nothing focused: TAB carries on from where focus last was, or focuses the first component
(SHIFT-TAB: the last).

**Everything else** (and keys while nothing has focus) goes through each `DefaultFocusManager`'s
routing chain:
1. focus requests (clicks, `RequestFocusEvent`)
2. the current child (the focus path)
3. a key: a child whose **hotkey** it is - it becomes current and gets the key
4. any other event: the **first** other child that claims it (§2.5)
5. the custom extra handler (`setCustomExtendedEventHandler`)

Per manager: its nav keys (`setPrev/NextKeyStroke`, `setNavKeysNone()` for none), its policy
(GROUP / ONE_STOP / TRAP), `setTabStop`, and for a ONE_STOP `setCycling(true)` to wrap around.
Disabled children are skipped; a manager is enabled if any child is.

(Before step 6 each manager navigated itself, ordered by a per-class "nav style"
`NAVKEYS_FIRST` / `LAST` / `NONE` with cycling inferred from its position - see §4.1 for what
that broke.)

### 2.3 Explicit focus requests — `REQUEST_FOCUS`

`RequestFocusEvent(target)` is raised on the bus. Every `DefaultFocusManager`
has a request-focus handler (`FocusManager.UTIL.buildRequestFocusEventHandler`)
that:
1. finds a path from itself to `target` (`findTargetAsPath`) — first downward
   through `getChildFocusables()`, then as fallback upward through
   `getParentFocusManager()`;
2. if found, sets focus on itself, then walks the path calling
   `setChildAsCurrentFocus` at each level.

If no path is found the event is not claimed and **nothing happens** (silently).

### 2.4 Mouse click → focus — `events/mouse/MouseEventOrchestrator`

1. RAW_MOUSE is turned into an `ApplicationMouseEvent` (canvas position).
2. `GetComponentAtOffset` finds the deepest component under the mouse.
3. Walk up the **component** tree (`getParentContainer()`) to the first
   `Focusable`/`ToFocusable`.
4. Raise `RequestFocusEvent(focusable)` → §2.3 resolves it in the **focus** tree.
5. If the component is `Clickable` / `Draggable`, deliver the click/drag with
   a component-relative position once it has focus.

Note steps 3 and 4 use two different trees (see §4).

### 2.5 The focus tree also delivers non-key events

`DefaultFocusManager` does more than keyboard focus. For any event that reaches it, it
offers the event to the current child, then (for non-key events) to the **other
children** too, stopping at the first claimer. (Before step 6 some managers - the root among
them - delivered to **all** claimers; no example ever had more than one.)

Components register many handlers on their **own internal chain**, not on the bus:
`TextInput.onChange`, `Button.onChange` (via `HasValue.UTIL.bindConsumerAsOnValueChangedHandler`),
a behaviour's `handleEvents(..)` (`TextInputWithIncrDecrButtons`, `MovingPicturesViewer`, `ScrollableComponent`). Example: `Button.onClick` raises a
`ValueChangedEvent` on the bus; it reaches the button's `onChange` action **only through the
focus tree**. The same route carries `VALUE_CHANGED`, `TABLE_ROW_SELECTED`/`TABLE_CELL_SELECTED`,
`VIEW_OFFSET_CHANGED` and custom events.

Consequences: a component missing from a stale focus tree doesn't just lose TAB, its
`onChange` handlers **never fire**; and any focus redesign must keep this delivery working.

Mouse is mostly separate: app-level `ApplicationMouseEventHandler`s sit on the bus, and
`MouseEventOrchestrator` calls `clickable.onClick()` directly. Only unclaimed mouse events fall
into the focus tree's "offer to children" loop.

### 2.6 TAB-consuming components

`TextInput` / `MultiLineTextInput` consume TAB and SHIFT-TAB when `multiLine` is true. `TabPanel`
once used TAB as a hotkey (now disabled: "a really bad idea because circumvents normal
FocusManager navigation"). Before the redesign, a `NAVKEYS_FIRST` focus manager took TAB first, so a
multi-line editor inside one never received TAB; now the focused component always gets the first say.

### 2.7 Inventory: what travels through the focus tree (Sep 2026)

Handlers attached to a **component's own chain** (not the bus). Their events reach them only by
travelling down the focus tree. A redesign must keep delivering every row.

| Event type | Registered by / where | What it does |
|---|---|---|
| KEYPRESS | ~50 `KeyInputEventHandler`s on component chains (TextInput, ListBox, Menu, ScrollBars, Table, MessageBox, ...) | per-component key bindings; they reach the component only while it (or an ancestor path) has focus |
| VALUE_CHANGED | `onChange` on TextInput, MultiLineTextInput, TextInputForType, Button, RadioGroup, TabButtonComponent, ListBox, DropdownListBox, TextInputWithIncrDecrButtons, ScrollBars, ImageManipulationControls (`HasValue.UTIL.bindConsumerAsOnValueChangedHandler`) | component actions (a Button's action is a VALUE_CHANGED handler) |
| VALUE_CHANGED (from children) | `FormBinder.onChange` on the **form panel** (ancestor) | form-level change handling: needs delivery through ancestors |
| VIEW_OFFSET_CHANGED | `HasViewPortWindow` (`newOnChangedHandler`) | viewport scroll sync |
| TABLE_ROW_SELECTED / TABLE_CELL_SELECTED | `TableComponent` (`rowSelectedHandler`, `cellSelectedHandler`) | table selection callbacks (row: clicks and, since Oct 2026, key moves) |
| REQUEST_SCROLL_TO_VIEW | `WindowViewComponent.ScrollToViewHandler` in `ScrollableComponent`, `WindowFrame`, `WindowFrameFocusable` chains (plus a bus-level `FindScrollToViewHandler` in `TUIApp`) | scroll a focused/requested component into view. **Focus side effect:** focusing inside a scrollable can scroll it (see comment in `MouseEventOrchestrator`) |
| any (forwarded) | `ScrollableComponent`: if the footer (e.g. search box) has focus and cannot handle an event but the viewed component can, it forwards the event **and then raises `RequestFocusEvent` for that component** | *event-driven focus*: typing moves focus to the component that consumes it |
| any | a behaviour's `handleEvents(..)` (`TextInputWithIncrDecrButtons`, `MovingPicturesViewer`, `ScrollableComponent`) - underneath, `customExtendedEventHandler` on its `DefaultFocusManager` | extra per-component handling |
| KEYPRESS (modal) | `ModalEventHandler` prepends itself to its parent container (bus or component), inner chain + keyboard sinkhole | modal dialogs swallow all keys |

On the bus (not via focus tree): idle/terminate handlers, canvas refresh, cursor show/hide,
resize, `FindScrollToViewHandler`, mouse orchestrator, component inspector,
`SearchFunctionForScrollableComponent`'s title updater (added to the bus at runtime).

---

## 3. How components get focus managers

### 3.1 Today (Oct 2026): a derived focus tree and held behaviours

There is no focus list to keep in step any more. A focus manager works out its children from its
component whenever it needs them: the nearest visible focusable components below it, with popups
(a dropdown's open list, a menu) counted under the component that owns them
(`FocusManager.UTIL.derivedChildFocusables`, `parentOf`; see §4.3 C). Adding, removing or hiding a
component later needs no call to anything.

How a component gets focus - three cases, nothing else to choose from:

| You are building | Do this | Examples |
|---|---|---|
| a layout of other components | a `Composite`: the fields inside are TAB stops anyway | any app's form panel |
| one TAB stop (a button, a check box) | hold a `LeafFocusBehaviour` | CheckBoxInput, TreeView, TableComponent, RadioGroup's options |
| a component made of focusable components | extend `ComponentWithFocus<Panel, Self>`; say how focus moves with `focus()` | ButtonBar, NavButtonsPanel, ScrollableComponent, MenuBar, MovingPicturesViewer, TextInputWithIncrDecrButtons |

`ComponentWithFocus` holds the panel that lays its components out and a `ParentFocusBehaviour`
(`focus()`); border, title, colours and hilite are the panel's. By default each child is a TAB
stop - nothing to set. Otherwise, in plain words:

```
focus().singleTabStop()                      TAB treats the whole component as one stop ...
       .arrowsMoveInside(ArrowKeys.UP_DOWN)  ... and these keys move between its children
       .wrapAround()                         past the last child goes back to the first
       .enterAt(Entry.FIRST_OR_LAST)         where focus lands when it arrives (or enterAt(() -> child))
       .skippedByTab()                       TAB passes it by (mouse and hot keys still reach it)
       .keepsFocusInside()                   TAB never leaves it (a modal dialog)
       .takeFocusItselfWhenNoChildCan()      nothing focusable inside: it is the stop itself (a scroll pane showing text)
       .handleKeys(..) / .handleEvents(..)   its own keys (those its children didn't use) and events
       .whenChildFocused / .whenFocusArrives / .whenFocusLeaves
```

A component that can't extend it (its panel has no border/title, e.g. an open menu's ListBox)
holds a `ParentFocusBehaviour` itself and hands it out (`ToFocusable.asFocusable()`,
`AsKeyboardHandler.asKeyboardHandler()`); the leaf behaviour is handed out the same way.
Keys reach a component through `KeyRouter` (§2.2): it handles keys with `KeyboardHandler.onKey`,
or hands out its usual event handlers adapted with `AsKeyboardHandler.UTIL.toKeyboardHandler`.

### 3.2 The older base classes

Gone (Oct 2026): `ComponentWithFocusManager`, `AbstractWrappedComponentWithFocusManager` - every
user moved to `ComponentWithFocus` or a held behaviour. `CompositeWithFocusManager` stays as the
**app's root panel** only: a Composite that is also the root focus manager on the bus (TUIApp's
default panel; the focus tests' roots) - not something to extend. Still to move: the windows and
dialogs (`WindowFrameFocusable`: MessageBox, DialogComponent, TabPanel) and the Clickable wrappers
(`AbstractWrappedComponentAndFocusable`). (The old bases used to register focusables with their
manager on add / remove - `BehaviourForCompositeAndFocusManager`, a snapshot that went stale when a
deeper component changed. That class is gone: the derived tree replaced it.)

### 3.3 The root — `TUIApp`

- `getApplicationFocusManager()` uses the main panel if it is a `FocusManager`
  (default `TUIDefaultPanel` is a `CompositeWithFocusManager`), otherwise builds
  one from the panel with `buildFocusManagerFromComponent`, and appends it to the bus.
- `verifyFocusables` defaults to **true**: after building, `verifyPathsToFocusables`
  walks the component tree, and for every focusable checks a path exists in the focus
  tree. If any is missing it **throws `IllegalStateException`** ("Failed to find focus
  path for: ..."). This is the "assert that fails on certain compositions".

---

## 4. Diagnosis: why nested focus breaks

### 4.1 Reproduced behaviour

Throwaway experiment (not in the repo) driving real `DefaultFocusManager`s with
`root[A, group[B, C], D]`. Expected TAB order `A B C D A B C D ...`:

| Setup | Actual TAB order | Defect |
|---|---|---|
| root `NAVKEYS_LAST`, group `NAVKEYS_FIRST` (typical TUIApp + ComponentWithFocusManager) | `A B C D A C D A C D` | after first lap **B is never reached** |
| root `NAVKEYS_LAST`, group `NAVKEYS_LAST` (ButtonBar style) | `A B C D A C D A C D` | same |
| root `NAVKEYS_FIRST`, group `NAVKEYS_FIRST` | `A B D A B D` | **C is never reachable** |
| SHIFT-TAB, typical setup (expected `A D C B`) | `A D B A D B` | **C skipped** |

Confirmed in the real running app with `examples/NestedFocusBugTest` (TextInputs,
`ComponentWithFocusManager` group, keys sent through a pseudo-terminal):

| Scenario | Keys | Actual |
|---|---|---|
| default panel | TAB ×12 | `A B C D A C D A C D A C D` |
| default panel | SHIFT-TAB ×12 | `A D B A D B A D B A D B A` |
| inside outer `ComponentWithFocusManager` | TAB ×12 | `A B D`, then TAB does nothing |

Two further findings from the real app:
- **No initial focus.** `TUIApp` never calls `setFocus(true)` on the root manager.
  Until something is clicked, `setChildAsCurrentFocus` only moves the `current`
  pointer (it focuses the child only `if (this.hasFocus())`), so TAB visibly does nothing.
- **Dead end at the root.** With a single nested group as the root's only child, the
  group has cycling off (inferred) and the root's `getNext` needs ≥ 2 children, so once
  TAB leaves the group there is nowhere to go and focus is stuck.

Causes:
1. **Two kinds of focus manager, one mechanism.** Nested focus managers come in two kinds
   that need opposite rules:
   - *Grouping focus manager* (form section, panel of fields): children are separate tab
     stops. TAB entering forwards should start at the first child, entering backwards at the last.
   - *One-stop component* (ListBox, DropdownListBox, menu, tab strip): the component is **one**
     tab stop, arrow keys move inside it, and re-entry **should** resume the remembered child.

   A `DefaultFocusManager` can't tell which kind it is. Re-entry always resumes the
   remembered `current` (one-stop behaviour), but if the manager's nav keys are TAB it also
   makes each child a tab stop (grouping behaviour). The result follows neither rule:
   first lap `A B C D`, then B is unreachable. Managers that navigate with other keys
   (`DropdownListBox` uses UP/DOWN) already behave correctly as one-stop components. The manager is
   also never told the direction focus arrived from, which a grouping focus manager needs.
2. **Nav-style mismatch across levels.** An outer `NAVKEYS_FIRST` manager takes
   TAB before the inner group sees it, so the inner group can't be traversed. Nav
   style is chosen per class, so how TAB behaves depends on which components you
   happened to compose.
3. **Cycling inference** only works if `setParentFocusManager` is called in the
   right order. An explicit `setCycling(true)` on a nested manager traps TAB inside it.

### 4.2 Structural causes

- **Two trees kept in sync by hand** *(fixed - step 7c: the focus tree is now worked out from the
  component tree whenever it is needed, see §4.3 C)*. The component tree (layout, hit-testing,
  clicks) and the focus tree (keyboard) are separate structures. The focus tree
  is a snapshot taken when children are added (§3.2). Any drift produces
  unreachable focusables (verify throws) or clicks that silently do nothing (§2.3).
- **Wrapper vs inner manager (reproduced Oct 2026, see below).** Component → focusable
  identity is stable, because every `asFocusable()` checked (~18) caches its instance lazily.
  The fragile part is the wrappers (§3.1): the tree is only consistent because each wrapper
  calls `setActual(wrapper)`, so children point at the wrapper and not at the inner
  DFM. A wrapper that skips this, or code that adds `wrapper.getFocusManager()`
  directly, gives the same node two identities, and path finding then depends on which
  direction it searches. The disabled `skipProxiedManagers` workaround in
  `verifyPathsToFocusables` suggests this has failed before.
  **Reproduced:** `ComponentWithFocusManager`'s constructor builds its inner DFM and adds the
  wrapped component's focusables *before* `setActual(this)`, so those children's parent is the inner DFM
  while the parent's child list holds the wrapper; walking up from a child never meets the wrapper.
  Today's path search survives via a fallback; the new `advance` uses
  `FocusManager.UTIL.containsFocusable`, which treats a wrapper and its provided manager as one node.
- **Distributed state.** Each level stores its own `current` and `hasFocus`. Nothing
  guarantees there is exactly one focused leaf, or that each ancestor's `current`
  points toward it.
- **State changes inside `canHandle`.** `getCurrentOrSet()` (sets a default current
  child) and hotkey handling both change focus inside `canHandle`. Chains call
  `canHandle` several times per event (the code notes "is this 3 tests now?"), so
  side effects can run more than once, or on events that are then never handled.
- **"Obscure bug" guards.** All three wrappers skip `setFocus/canHandle/handle` when the
  inner manager has 0 children. This works around empty-manager behaviour instead of
  fixing it.

### 4.3 Design (being implemented step by step - each point says its status)

Two jobs are tangled in today's focus tree; the proposal separates them.

**A. Focus & navigation**

Terms used below:
- *focus*: the component that receives keystrokes (e.g. the text field the user is typing in).
- *focus manager*: a `FocusManager` - a node in the focus tree that holds focusable components and
  manages focus among them (a form panel's, a button bar's, a dropdown's, a window's). Not the same as a component container (`Composite`): the two trees are separate (§4.2).
- *nav keys*: keys that move focus rather than being typed, e.g. TAB / SHIFT-TAB; each focus manager
  can have its own (a dropdown uses UP / DOWN).

1. **One owner of focus.** *(done - step 4, in this form:)*
   "Who has focus?" used to be answered by combining many separate pieces of state: every focus manager
   remembers its own "current child", and every component keeps its own `hasFocus()` flag. These
   could disagree - e.g. after a click, a dropdown's button still reported `hasFocus()==true` while
   an image viewer actually had focus (§5).
   Now the ROOT focus manager keeps **one** field, "the focused component"
   (`DefaultFocusManager.getFocusedComponent()`), updated by every focus move - `moveFocus` and the
   old routing alike - and `FocusManager.UTIL.findCurrentFocus(root)` answers from it. When focus
   moves, `moveFocus` also checks the previous owner really let go: if it still claims focus (missed
   its `unFocus`), it is switched off - so exactly one component claims focus.
   Still there, but no longer the source of truth: components' `hasFocus()` flags (display state:
   hilite) and focus managers' "current child" (a one-stop component's memory of its last child). Removing them
   entirely - `hasFocus()` meaning "am I the owner?" - would touch every focusable component; only
   worth doing if they cause trouble again.

2. **Who gets a key first.** *(done - step 2; now `KeyRouter`, see §2.2)*
   When a key arrives:
   1. If it is not a nav key of any focus manager around the focused component, it is ordinary input
      and goes where it always did.
   2. If the focused component wants it anyway, it gets it - e.g. a multi-line editor that inserts
      TAB characters. *(Planned: such an editor releases TAB again after Esc.)*
   3. Otherwise the focus managers around the focused component are asked, innermost first. A
      ONE_STOP first offers the key to its own handlers (e.g. a dropdown's "DOWN opens my list");
      then each is asked where focus should go (point 3). *(done - step 5: one-stop components' own keys)*
   So the most specific wish wins: the focused component, then a component's own keys, then moving
   focus. The order lives in `KeyRouter` only; a component just declares its policy, its nav keys
   and its own keys (`KeyboardHandler.onKey`, or appended handlers handed out via `AsKeyboardHandler`).

3. **Focus managers answer two questions; they decide, their parent doesn't.** *(done - steps 1 and 2)*
   - `enter(direction)`: "focus is arriving in you, moving forward (TAB) or backward (SHIFT-TAB) -
     which of your components should get it?" Returns that component, or null for "nothing in here
     can take focus, skip me" (empty, disabled, or not a TAB stop).
   - `advance(focused, direction)`: "a nav key was pressed while focus is inside you - where next?"
     Answers MOVED(component), DONE ("no more in that direction - ask my parent") or STAY ("handled,
     focus stays").
   How a focus manager answers depends on its *policy*:
   - **GROUP** - each child is its own stop (a form section: TAB visits every field). Entered
     forward it starts at its first child, backward at its last. The outermost focus manager wraps
     around instead of answering DONE.
   - **ONE_STOP** - the whole focus manager is one stop (dropdown, list box, tab strip). Entering it
     returns to the child it had last time. Its own arrow keys move inside it; at either end focus
     stays (STAY: the key is used up, it does not go further out), unless the component is set to wrap
     around (`setCycling(true)`, e.g. the tab strip). *(Step 5: one-stop components' own keys go through these
     answers too.)*
   - **TRAP** - focus cannot leave (modal dialog): navigation wraps around inside.
   A focus manager - or a single component - can also be marked *not a TAB stop* (`setTabStop(false)`):
   TAB skips it, clicks and hot keys still work - e.g. the scroll buttons under a scrollable panel,
   and a window's title bar and resize corner (so TAB goes straight to the window's content).
   Example, form `A, [B, C], D` where `[B, C]` is a GROUP: TAB goes A B C D A B C D...; if `[B, C]`
   were a ONE_STOP, TAB would go A B D A B D... and arrow keys move between B and C.

4. **One way to move focus.** *(done for clicks and TAB - step 3, `FocusManager.UTIL.moveFocus`)*
   `moveFocus` changes focus and sends the existing notifications: `unFocus(new)` to what loses focus,
   then `focus(old)` to what gains it, along the path (hilite, bring-to-front and the dropdown / menu
   hand-off depend on these, §4.4). It goes through each focus manager's `setChildAsCurrentFocus`, so
   components that customise that method keep working. It works in two passes: focus managers that don't
   have focus yet first just record which child to use (deepest first, no notifications); then focus
   is passed down top-down and arrives straight at the target. Without the first pass a focus manager
   receiving focus handed it to its remembered child (or its first child) first - a flicker, and in
   `ImageSplitterTest` a window wrongly brought to front.
   *(Not through `moveFocus`: hot keys, and code calling `setChildAsCurrentFocus` directly.)*
5. **Clicks use the same `moveFocus`**, with no intermediate focus on other components (§4.6).
   *(done - step 3; clicks still first re-send `setFocus(true)` to re-assert hilite / front, §4.4)*
6. **Initial focus**: when the app starts, focus goes to the first focusable component, with a way to
   choose another (the common convention: desktop toolkits, Textual). *(done - step 4)* The focus
   manager owns it: `DefaultFocusManager.setInitialFocus` / `setAutoFocus` / `giveInitialFocus`.
   `TUIApp` only forwards settings to its root manager and calls `giveInitialFocus` once startup is
   done; a dialog can call it when shown.
7. **Asking never changes anything**: `canHandle`, `enter` and `advance` only answer.
   *(done - steps 3 and 4: `DefaultFocusManager.canHandle` no longer picks a current child or switches
   to a hot key's component; those changes now happen in `handle`)*

**B. Event delivery for non-key events (§2.5): same semantics as today**
1. `canHandle`/`handle` remain the generic contract for **every** event type.
2. Keep today's path and order: from the root down through the focus managers, focused path
   first, then the first other child that claims it *(step 6: first claimer everywhere)*.
   Focus managers are discovered on demand *(done - step 7c)*.
3. **No "targeted" delivery straight to the event's source.** Ancestors listen to their
   children's events: `FormBinder.onChange` registers on the *form panel*, an ancestor of the
   inputs, so skipping ancestors would break form-level change handling.

**C. Shared: on-demand discovery.** *(done - steps 7a-7d)* Neither job keeps a stored list; both
walk the component tree when needed (nav keys, clicks, drag start/end, broadcasts). Nothing goes
stale, and `rebuildFocusManager()` / `registerAllFocusables()` are deprecated (no longer needed).
- A focus manager's **children** are the nearest visible focusables under its component
  (`FocusManager.UTIL.derivedChildFocusables`); a focusable's **parent** is found by climbing the
  component tree to the nearest focus manager (`FocusManager.UTIL.parentOf`, using
  `Focusable.getFocusComponent` to get from a helper object to its component). Hidden components
  are skipped; a removed one climbs to nowhere.
- **Popups** (a dropdown's open list, menus) are drawn on the root panel - never clipped by their
  container, never growing it - but `Component.getPopupOwner` makes them part of their owner for
  focus: they are left out where they are drawn and listed right after their owner.
- A **focus manager built by hand** (no component behind it) keeps the list it was given with
  `addFocusable`; nothing is enforced.
- No caching: a full walk of a 1,000-input screen measured ~42 microseconds.

**D. Migration.** The engine (A and B) switches in one commit; components migrate
gradually, because leaves inherit defaults and most managers extend three base classes. Order:
inventory the event types routed through the focus tree → safety-net tests (incl. "button
`onChange` still fires", dropdown/menu hand-off) → add interfaces with defaults → switch
the engine → migrate classes one by one. Work on branch `focus-redesign`.

**Decisions (Sep 2026):**

| Question | Decision |
|---|---|
| `ButtonBar` | current behaviour: GROUP (TAB through buttons, leave at the ends); `TabPanel`'s strip keeps `setCycling(true)` |
| `MultiLineTextInput` TAB | moves focus by default; opt-in to insert `\t`, then **Esc** leaves (Textual `TextArea` `tab_behavior` model). Alt-TAB is grabbed by window managers; Ctrl-TAB is indistinguishable from TAB in most terminals |
| Event delivery | today's path and order, discovered on demand; no targeted delivery |
| Broadcast rule | **first claimer everywhere** (how the bus's main chain works too). Changed the root, which delivered to all claimers - none of 14 examples ever had more than one. *(done - step 6)* |
| Focused component hidden/disabled | move focus to the next focusable (else previous, else none); triggered via `setEnabled(boolean, EventBus)` / hide |

This follows the same approach as browsers (tab order derived from the DOM, roving
tabindex for one-stop controls) and most modern TUI toolkits.

### 4.4 Constraints any redesign must keep

- **Navigation keys are configurable.** Never hardcode TAB/SHIFT-TAB; use each
  focus manager's configured nav keys (`DropdownListBox` uses UP/DOWN, for example).
- **The child decides.** A parent offers the nav key to its current child first. The
  child answers "moved internally" or "done", and the parent moves on only when the
  child is done. Entry carries a direction, so a grouping child can pick its first or last
  child and a one-stop component can resume its remembered one. A child with nothing focusable
  (empty, disabled, and possibly hidden) answers "skip me".
- **Keep the cooperative hand-off.** `unFocus(newFocusable)` is load-bearing:
  `DropdownListBox` stays open when focus moves into its own list, and `Menu`/`MenuBar`
  stay popped up when focus moves within the menu (`isFocusablePartOfMenu`). Keep the order:
  unfocus the old component with the new target known, *then* focus the new one. Keep the meaning:
  `newFocusable` is the new sibling at that level, which may be a nested manager, not
  necessarily the leaf. `focus(oldFocusable)` is currently unused (it's only forwarded or logged).
- **Discover the tree only when needed.** Ordinary keypresses go straight to the
  focused component, held as a direct reference. The component tree is walked only for navigation keys,
  clicks, drag **start** and drag **end**, never on each drag-move event.
  - Today an active drag already bypasses focus work (`MouseEventOrchestrator`,
    `currentDraggable != null`). But a drag that didn't capture a draggable
    runs a hit-test, a walk up and a `RequestFocusEvent` on **every** `DOWN_LEFT_DRAG`.
    Fix that; don't inherit it.
- **Re-assert focused state on the whole new path, on every focus move.** Today the request-focus
  handler calls `setFocus(true)` on the root before walking the path, which re-sends
  `setFocus(true)` down the currently focused chain: frames re-hilite, `AbstractWrapped...`
  components re-`bringToFront`. That is the only thing that restores z-order / hilite of ancestors
  that *stay* focused (e.g. clicking another field in the same window after a popup or another
  window was brought in front). Keep that effect deliberately: on each move, ancestors on the new
  path re-assert hilite / front, not just the nodes that changed. (Side effect today: the leaf about
  to lose focus first gets a redundant `setFocus(true)`; harmless except for components that do work
  in it, e.g. `DropdownListBox` opens its list.)
- **Focus notifications drive visible side effects: keep them all.** `moveFocus` must call
  `unFocus(new)` on every node of the old path below the split, and `focus(old)` on every node of the
  new path, **top-down, focus managers included**. That is how `WindowFrameFocusable` hilites/unhilites
  its border, `AbstractWrappedComponentWithFocusManager` does `bringToFront`, and
  `DefaultFocusableBehaviour` recolours components. Keep the within-node order too (e.g.
  `WindowFrameFocusable.focus` passes focus down first, then hilites itself).
- **The child decides; the parent never inspects the policy.** `advance` answers MOVED(target),
  DONE (bubble up) or STAY (handled, no move, e.g. DOWN at the end of a list). `moveFocus` *tells*
  focus managers "focus is now inside you at child X" (cf. `childHasCurrentFocus`), and each
  focus manager decides whether to remember it.
- **A popup belongs to its owner for navigation.** A dropdown's list or a menu's popup may be
  attached elsewhere in the component tree (root panel overlay); leaving it with a nav key must
  behave as leaving the owner in that direction.
- **Existing modal trap:** `ModalEventHandler` is prepended to the bus and swallows all keys
  (keyboard sinkhole). It is separate from focus managers and must keep working.
- **Remembered state belongs to the component** (for example, a ListBox's selected index), not to a
  stored list in a manager. If the remembered child is gone, disabled or hidden, fall back
  to the first child.
- **Visibility:** the focus code currently has no notion of hidden components.
  Untested whether hidden components (for example, inside a collapsed `CollapsiblePanel`) stay tab stops.
- **Wrap-around may re-enter the starting child.** When a wrapping focus manager (root or
  trap) comes back round to the child that just reported "done", it must re-enter that child
  from its first/last leaf instead of giving up. Otherwise a root whose only child is a group dead-ends,
  which is today's bug, and the first prototype had it too.
- **Open question:** when the *focused* component becomes hidden or disabled, who moves focus
  off it, and when? Currently nothing does; the next nav key just restarts from the top.

### 4.5 Prototype result (Sep 2026)

A standalone prototype of §4.3 and §4.4 (removed once the real implementation replaced it; one focus root, children discovered
on demand, `enter(dir)`/`advance(dir)`, four kinds of focus manager: transparent panel, group,
one-stop, trap) passes all 20 scenarios:

- grouping: `A,[B,C],D` gives `A B C D A B C D…`, and SHIFT-TAB gives `D C B A…`
- one-stop: one tab stop, arrows move inside, re-entry resumes the remembered child (also after a click)
- a single nested group as the root's only child gives no dead end
- plain panels are transparent (flattened); disabled, hidden and empty focus managers are skipped;
  a disabled remembered child falls back to the first
- trap cycles inside; custom nav keys (RIGHT/LEFT) coexist with TAB
- a child added at runtime is reachable with no rebuild call
- hand-off: `ddText.unFocus(new=ddList)` keeps the dropdown open, and `ddList.unFocus(new=D)` closes it
- 1,000 ordinary keypresses cause 0 tree walks

Not covered by the prototype: **non-key event delivery (§2.5), since it routes keys only**; reserved escape keys; real mouse and drag plumbing, the `REQUEST_FOCUS` event path,
hotkeys, non-key events routed to non-focused children, canvas refresh, and fitting it
into the existing `FocusManager` API and wrapper classes.

### 4.6 Click focus flicker (today)

Clicking B in `A,[B,C],D` while D has focus: the root calls `setChildAsCurrentFocus(group)` →
`group.focus(D)`, which re-focuses the group's **old** current child (say C), and only then
`group.setChildAsCurrentFocus(B)` → `C.unFocus(B)`, `B.focus(C)`. C gets a spurious
focus/unfocus pair. For dropdowns and menus that react to focus, this is a likely source of
intermittent open/close glitches. Not yet reproduced in a test.

### 4.7 Secondary objective: simplify the component base classes

**Status (Oct 2026):** done in a different form than first planned - delegation, not new base
classes: components hold `LeafFocusBehaviour` / `ParentFocusBehaviour` (§3.1). RadioGroup and
TreeView were built on them as the two proofs; CheckBoxInput, DropdownListBox and TableComponent
moved over. ScrollableComponent2 replaced the old ScrollableComponent (renamed back to
`ScrollableComponent`). Then (Oct 2026) one base for components made of focusable components,
`ComponentWithFocus`, with the focus settings in plain words (§3.1); the old bases went, except the
app's root panel and the windows / Clickable wrappers still to move (§3.2). The text below is the original plan.

Once focusables are discovered on demand (§4.3 C), components no longer have to keep a focus
list in step with their children. That removes the main reason several mid-level base classes
exist, so the redesign should **reduce them where it can be done carefully**.

Current hierarchy (focus-related; subclass counts exclude examples):

```
AbstractComponent                                   (15 direct subclasses)
 ├─ Composite                                       (10)   layout only, no focus
 │   ├─ CompositeWithFocusManager                   (2)    is a Composite + owns a focus manager
 │   └─ ScrollableComponent                         (0)    own non-standard focus handling
 ├─ ComponentWithFocusManager                       (4)    wraps a Composite + owns a focus manager
 │   └─ ButtonBar, DropdownListBox, ScrollableComponent2, ...
 ├─ AbstractComponentWrappper                       (13)   wraps one component
 │   └─ AbstractWrappedComponentAndFocusable        (7)    wrapper that is focusable
 │       └─ AbstractWrappedComponentWithFocusManager (7)   wrapper that owns a focus manager
 ├─ WindowFrame / WindowFrameFocusable              (0)    two near-parallel classes
 └─ ...
```

Problems:
- **"Which one do I extend?"** Three different bases mean "container with focus"
  (`CompositeWithFocusManager`, `ComponentWithFocusManager`, `AbstractWrappedComponentWithFocusManager`),
  plus `ScrollableComponent` doing it a fourth way. They differ in nav-style defaults (§2.2).
- **Inconsistent child/focus bookkeeping.** `CompositeWithFocusManager.add` registers focusables;
  `ComponentWithFocusManager.add` does **not** (only `addComponentAndItsFocusables` does);
  the wrappers need an explicit `registerAllFocusables()`. Same operation, different results.
- Inheritance is deeper than it needs to be.

What must be kept (the owner's stated goals):
- **Convenience that steers developers to the right thing.** Base classes (or delegation helpers,
  "services" in the PowerBuilder PFC sense) exist to remove boilerplate so that the easy way is the
  correct way. Removing a base class must not push that boilerplate onto every component.
- Abstract classes are fine where they earn their place; the aim is fewer and clearer ones, not none.

Related goal (owner, Oct 2026): **"attach scrollbars to anything and scrolling just works"**.
`ScrollableComponent` and `ScrollableComponent2` overlap (the second was added for a long-forgotten
problem and extends a newer base); expect one to be removed, or both replaced, as part of this
cleanup. Neither is worth investing in until then.

Approach: after the engine switch, list each base class with its subclasses and what it still does;
then merge or remove only where the remaining behaviour is a focus *policy* (GROUP / ONE_STOP / TRAP)
that a plain container can declare. Do this class by class, not in the engine commit.

---

## 5. Wart list (event/focus area)

| Where | Issue |
|---|---|
| `events/EventHandlerContainer.java` | **fixed (Oct 2026):** `Simple.prependEventHandler` appended and `appendEventHandler` prepended (swapped). `prependTo` now keeps the container's order. |
| `events/ChainedEventHandler.java` | **fixed (Oct 2026):** removing a pending prepend never worked (`!contains` inside a `contains` check). |
| `events/ChainedEventHandler.java` | **fixed (Oct 2026):** the public `ChainedEventHandler(EventHandler)` constructor left the strategy null (NullPointerException on the first event); it is a chain-of-responsibility chain now. |
| `events/ChainedEventHandler.java` | **fixed (Oct 2026):** `TRACE_AND_LOG_EVENT_HANDLERS` was `true` - every handler test for every keypress went to the log (the 200 MB `dtbTerminal.log`). Now `false`. |
| `events/EventBusImpl.java` | **fixed (Oct 2026):** with the queue full an event was dropped silently; now it is logged (still dropped after a 25 ms wait). |
| `events/ConditionalEventHandler.java` | **fixed (Oct 2026):** `AbstractDelegate.setName` ignored its argument. |
| `events/focus/DefaultFocusManager.java` | **fixed:** `setChildAsCurrentFocus(null)` threw a NullPointerException; it now refuses with an IllegalArgumentException saying so. |
| `events/focus/DefaultFocusManager.java` | **removed (step 6):** nav styles (field default `NAVKEYS_LAST`, constructor `NAVKEYS_FIRST`), cycling inference, the per-manager navigation |
| `events/focus/DefaultFocusManager.java` | **removed (step 4):** dead `getPrevOLD` / `getNextOLD` |
| `components/BehaviourForCompositeAndFocusManager.java` | **gone (Oct 2026):** its insert-index bug went with the class (the derived focus tree replaced it). |
| `controlcodes/ControlCode.java` | **fixed (Oct 2026):** several mouse mode codes (`SET_*` and `DISABLE_*`) had an extra digit (`[?10021l` for `[?1002l` ...). Only SGR mode (`-Ddtb.legacyMouseMode=false`, off by default - it has other bugs) used a broken enable code. |
| `components/ButtonBar.java:77-80` | **fixed (Sep 2026):** `setNextKeyStroke` called `setPrevKeyStroke`, so "next" stayed TAB and the prev key was overwritten (e.g. `TabPanelTest`'s Ctrl-Left/Ctrl-Right never worked) |
| `components/tab/TabPanel.java` | **fixed (Sep 2026):** tab strip navigated with TAB + `setCycling(true)` (a focus trap), and activating a tab always moved focus into its content, so strip navigation stopped after one step. Now: on the strip, Left/Right (wrapping) show tabs and focus **stays on the strip**; TAB goes strip → content → `<-` `->`; click, tab hot key and the prev/next shortcuts (`setPrevKeyStroke`/`setNextKeyStroke`) show the tab **and** focus its content. The shortcuts work from anywhere inside the panel but sit last in its chain (`customExtendedEventHandler`), so the content gets the key first |
| `components/input/DropdownListBox.java` | **fixed (step 7c):** the open list is drawn on the root panel (never clipped, never grows its container) but counts as part of the dropdown for focus (`Component.getPopupOwner`). The interim TAB re-raise hack is gone. |
| `components/input/DropdownListBox.java` | **fixed (Oct 2026):** clicking elsewhere while the list was open left it open. The dropdown already got its `unFocus` when focus moved into the list, so a later focus move only unfocused the list. In windowed apps the list then hid *behind* the window (bring-to-front re-send) but stayed open and in the tab order. Now `ListBox.onUnFocus` (new protected hook) lets the dropdown close the list once the move has finished, if neither the list nor the dropdown has focus |
| `components/input/DropdownListBox.java` | **changed (Oct 2026):** ENTER / Esc in the list commit the selection, give focus back to the dropdown and close the list (before: the list stayed open, hidden behind the dropdown's panel). |
| `components/modal/ModalEventHandler.java` | modal dialogs only trap **keys**; mouse clicks behind the dialog still go through and move focus out of it (seen in `ImageSplitterTest`'s Options dialog). Owner note: modals are also hard to create. Both for later |
| focus flags (distributed state) | **observed (Oct 2026):** two components reporting `hasFocus()==true` at once. In `ImageSplitterTest`, after clicking from a dropdown's open list (inside the Options dialog) onto the image, the dropdown's button still had `hasFocus()==true` while the viewer had focus. Concrete evidence for a single focus owner (§4.3 A). `DropdownListBox` now asks the root focus manager instead of trusting flags |
| `events/focus/FocusManager.java` (`buildRequestFocusEventHandler`) | **keep (Oct 2026):** each click re-sends `setFocus(true)` down the currently focused chain before moving focus. Looks redundant but re-asserts hilite / bring-to-front of ancestors that stay focused; see §4.4 |
| `events/focus/DefaultFocusManager.java` (`getCurrentOrSet`) | **fixed (step 3):** a container with no current child picked child 0 and focused it - on the first click, and whenever its current child had been removed (e.g. a dropdown's list closing), for **any** event reaching it. In `ImageSplitterTest` that brought the wrong window to front. Now clicks / TAB go through `moveFocus` (records the path first), and non-key events never pick a child. Key events in the old routing still may |
| `components/scroll/ScrollableComponent.java` (old) | **gone (Oct 2026):** the old class was deleted; `ScrollableComponent2` was renamed `ScrollableComponent`. |

| `components/input/DropdownListBox.java` - "leftover border" | **fixed (Oct 2026), old - seen on master too:** after ENTER / ESC in the open list a bottom border row stayed on screen. Not a drawing bug: the list was never closed. ENTER / ESC gave focus back to the dropdown's button (the `hideListBox()` calls were commented out); focus coming back brings the dropdown's panel to the front, *over* the still-open list, so only the list's bottom row - below that panel - stayed visible. The handler now closes the list once focus is back on the button. (Checked on the way and fine: the dirty flag, panel sizes, the canvas and the changed-only screen update.) |
| `events/focus/DefaultFocusManager.java` - "Bad state: offset null" | **fixed (Oct 2026), from step 7c:** `NullPointerException: Bad state: offset null for TextComponent[PLOT_...]` (TabPanelTest, ImageSplitterTest dialogs). Non-key events were offered to the remembered *current* child read raw - which could be a removed component, e.g. a dropdown's old scroll pane from a previous opening of its list. That pane still named the list as its view source, so it claimed the scroll-to-view request, then could not find the item climbing up (it is in the new pane). Non-key routing now uses `getCurrentChildFocus()`, which only returns the remembered child while it is still a child. Pinned by `DynamicStructureCharacterisationTest.testRemovedFocusedComponentGetsNoMoreEvents`. |
| `components/menu/Menu.java` (`isFocusablePartOfMenu`) | **simplified (Oct 2026):** the hand-written "is this part of my menu?" check is now `FocusManager.UTIL.containsFocusable` on the menu's list (submenus at any depth count, being part of the menu that opened them). Its submenu branch could never match (it compared the focusable with a `MenuContainer`). Pinned by two new two-level-submenu tests. |
| `components/input/RockerButton.java` | **gone (Oct 2026):** toggling left both its ON and OFF parts claiming focus. RockerButton was removed; `RadioGroup.onOff(...)` replaces it. |
| `components/table/TableComponent.java` (column sizing) | **open (Oct 2026), old:** on a short screen (about 20-30 rows) Table01Test's right-hand table in the horizontal view shows no cell contents: the column sizing doesn't give the long "Bio" column enough room. Confirmed on the old build. |
| `examples/ExecTest.java` | **open, old:** terminal control codes in a process's output are shown raw. |
| `examples/FrameSetTest.java` | **open, old:** drag-resizing the frames doesn't work properly. |
| sprites (plot) | **open, old:** a NullPointerException seen with sprites (details not recorded - reproduce first). |
| focus cue on borders | **open:** a bordered container doesn't show that focus is inside it (no border focus cue). |
| examples (demo infrastructure) | **open:** `Demo` / `SimpleDemo`, `LineChartDemo`, `BarChartRandomDataDemo` are to be replaced in the example clean-up. |
| focus tree "where focus was" | **idea:** cycles in the focus / component tree should fail fast with a readable component tree, not loop or be guarded silently. |
| `components/menu/Menu.java` | **open (Oct 2026), probably old:** an open menu stays open when the user clicks an empty, non-focusable area (e.g. a text display): the click moves focus nowhere, so nothing tells the menu to close (needs a "click outside" hook - same area as the modal mouse issue). **Fixed:** ESC in a menu now closes it (all levels) and focus goes back to its menu button. |
| `components/scroll/ScrollableComponent.java` | **fixed (Oct 2026):** the footer buttons' hot keys (DOWN = `v`, RIGHT = `»` ...) moved focus onto a (non-TAB-stop) footer button whenever nothing inside the pane took the key. As hot keys they even worked from outside the pane. The buttons have no hot keys now (they stay for the mouse); the pane scrolls for those keys itself while focus is inside it. |
| `components/input/DropdownListBox.java` (`calcAndSetMaxItemWidth`) | **fixed (Oct 2026):** an empty item list crashed (`Optional.get()` on the max width). |
| `examples/DropdownListBoxSimpleTest.java` (`PackageScanner`) | **gone (Oct 2026):** the helper that filled the demo lists from package folders (empty inside a jar) was removed; the demo uses fixed lists. |
| `examples/StdoutViewerTest.java` | **gone (Oct 2026):** the "MVN Content" button (a hard-coded path to another project) was removed. |
| `components/scroll/ScrollableComponent.java` | **fixed (Oct 2026):** `setBorder(boolean)` acted on the invisible outer box. A scroll pane always shows its border: `setBorder(false)` is now refused. |
| `textsegments/ScreenLineSegment.java` (text width) | **open, design limit:** a segment's width is its number of Java `char`s - one char, one column. Wrong for wide characters (CJK, most emoji: one char, two columns) and for characters outside Java's basic range (two chars, one column). Everything the project ships is ASCII plus single-column symbols (box drawing, `█`, braille). **Not to be changed without discussion first:** measuring columns means scanning text, which costs performance, and the segment merging / splitting code (`ScreenLineSegment.UTIL` and others) relies on length = width - it needs a well thought-out design. |
| `components/composite/Composite.java` (border clipping) | **fixed (Oct 2026):** a bordered Composite that grows with its children did not clip them to the inside of its border (only the fixed-size mode did). A child placed relative to the Composite itself (e.g. `MovingPicturesViewer`'s Options button, top-right) landed on the border; the overlapping segments made a screen row longer than the terminal, which wrapped and misaligned everything after it (TreeViewTest's animated images smeared the screen). Both drawing paths (`drawParts`, `drawChildren`) now clip children inside the border in both modes. Pinned by `CompositeBorderClipTest`. |
| `core/Canvas.java` (`outputRow`) | **open, deliberately not guarded yet:** overlapping segments on a row are printed one after another, so a row can be longer than the canvas; nothing below (screen backend, `LineTrackingCLIEmmitter`) clips to the terminal width. Left unguarded on purpose so component bugs (like the one above) stay visible - find the component first. |
| `core/Canvas.java` (`outputRow`: `row != this.height`) | **fixed:** rows count from 0, so the check was always true and a newline followed the last row too; and a blank canvas wrote one row too many (`outputBlankLines(0, height)`, inclusive). The two cancelled for a blank canvas only. Now the last row has no row end, a blank canvas writes `height` rows, and inline mode's exit (`InlineScreenMode.handBackTerminal`) writes the one newline that puts the prompt below the canvas. |
---

## 6. Characterisation tests (focus safety net)

Tests that record what the event/focus system does **today**, so the focus redesign cannot change
behaviour silently. A failure means "behaviour changed": decide whether the new behaviour is wanted,
then update the expectation deliberately. Tests named `..._today_...` pin known bugs; they are
expected to fail when the redesign fixes them.

| Test class | Covers |
|---|---|
| `events/focus/FocusCharacterisationTest` | nested TAB / SHIFT-TAB order (real components), button `onChange` via the focus tree |
| `events/focus/FocusProtocolCharacterisationTest` | exact `focus` / `unFocus` / `setFocus` calls and order, click flicker, TAB with nothing focused, disabled skip, root dead end, non-key delivery (first claimer) |
| `components/FocusSideEffectsCharacterisationTest` | frame hilite on click and TAB (frame TAB order: title, content, resize anchor), TextInput recolour, scroll-to-view request |
| `components/input/form/FormChangeCharacterisationTest` | typing -> data -> field `onChange` -> form `onChange` through the form panel |
| `components/input/DropdownListBoxCharacterisationTest` | open on focus, hand-off into the list, TAB / SHIFT-TAB / ENTER in the list, click elsewhere closes, DOWN opens a closed list, arrows at the ends |
| `components/modal/MessageBoxFocusCharacterisationTest` | TAB / SHIFT-TAB inside a real modal dialog (TRAP: wraps, never leaves); the field focused before keeps its flag (TODAY) |
| `components/DynamicStructureCharacterisationTest` | fields added / removed / hidden after the screen is built, inside nested panels - no rebuild calls |
| `components/input/DropdownPopupCharacterisationTest` | the dropdown's list as a popup: drawn on the root panel, container keeps its size, TAB carries on after the dropdown, keys reach it inside a dialog |
| `components/menu/MenuFocusCharacterisationTest` | menus: open, DOWN / RIGHT / ENTER / ESC / TAB / SHIFT-TAB inside, focus back on the button, click elsewhere |
| `components/HotKeyCharacterisationTest` | hot keys reach other components (typed as new KeyStrokes), incl. a parent component's hot key (RadioGroup.onOff) |
| `events/focus/OneStopNavigationTest` | a one-stop component's own keys: own handler before moving focus, STAY at the end, wrap when set to cycle, TAB leaves |
| `components/tab/TabPanelCharacterisationTest` | click / arrows / TAB / shortcuts |
| `components/modal/ModalEventHandlerCharacterisationTest` | modal key sinkhole |

`supportfortesting/SyncEventLoop` is a synchronous stand-in for the event bus: events go to one root
handler (bus-level handlers are not involved), `then(..)` callbacks run after each event, `asynch`
tasks run when the queue is empty, repeating timers are ignored.

Not covered yet: menus (popups on a live canvas), `ScrollableComponent` footer focus forwarding,
actual scrolling, hotkeys, table selection events, bring-to-front.

## 7. TODO (parked)

- **Scripted regression run for the examples ("Selenium for the TUI").** Scenario files (keys,
  clicks on *text*, drags, dialogs) played in a real pseudo-terminal; fails on any exception and on
  differences from recorded "golden" screens and focus order; `--record` to accept an intended
  change. Parked (Oct 2026) until the examples settle down - they are about to change a lot, and
  goldens would need constant re-recording. A working prototype of the driver exists from the focus
  redesign sessions (Python + `pyte`, dev-only); a pure-Java variant inside `mvn test` is the
  alternative (it would not see screen-backend bugs).
