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

### 2.2 `DefaultFocusManager` keyboard routing

For each KEYPRESS the manager decides between *navigation* (TAB / SHIFT-TAB by
default) and *forwarding* to its current child. The order is set per manager:

- `NAVKEYS_FIRST` — check nav keys first; forward only if not a nav key.
- `NAVKEYS_LAST` — forward first; treat as nav only if the current child declined.
- `NAVKEYS_NONE` — never navigate (hotkeys and forwarding only).

Other behaviour:
- **Cycling:** wraps from last to first. *Inferred*: a manager with a parent
  switches cycling **off**, so that TAB can "fall out" of a nested group to its parent.
- **Hotkeys:** if a non-current child declares a hotkey matching the key, it
  becomes current (this happens inside `canHandle`).
- **Non-key events** (e.g. mouse) go to the current child, or else to any child that claims them.
- Disabled children are skipped by `getNext/getPrev`; a manager is enabled if any child is.

Defaults vary by where the manager is built:

| Built by | Nav style | Cycling |
|---|---|---|
| `new DefaultFocusManager()` / `ComponentWithFocusManager` | `NAVKEYS_FIRST` | inferred |
| `CompositeWithFocusManager` (incl. `TUIApp.TUIDefaultPanel`) | `NAVKEYS_LAST` | inferred |
| `TUIApp.buildAndAttachFocusManager` (root) | `NAVKEYS_LAST` | inferred (on for root) |
| `ButtonBar` | `NAVKEYS_LAST` | off |
| `DropdownListBox` | UP/DOWN keys instead of TAB | inferred |

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
children** too. With `NAVKEYS_FIRST` it stops at the first claimer; with `NAVKEYS_LAST`/`NONE`
it delivers to **all** claimers.

Components register many handlers on their **own internal chain**, not on the bus:
`TextInput.onChange`, `Button.onChange` (via `HasValue.UTIL.bindConsumerAsOnValueChangedHandler`),
`customExtendedEventHandler` (`TextInputWithIncrDecrButtons`, `MovingPicturesViewer`,
`AbstractWrappedComponentWithFocusManager`). Example: `Button.onClick` raises a
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
FocusManager navigation"). Inside a `NAVKEYS_FIRST` container the container takes TAB first,
so a multi-line editor there never receives TAB. Behaviour depends on nesting.

### 2.7 Inventory: what travels through the focus tree (Sep 2026)

Handlers attached to a **component's own chain** (not the bus). Their events reach them only by
travelling down the focus tree. A redesign must keep delivering every row.

| Event type | Registered by / where | What it does |
|---|---|---|
| KEYPRESS | ~50 `KeyInputEventHandler`s on component chains (TextInput, ListBox, Menu, ScrollBars, Table, MessageBox, ...) | per-component key bindings; they reach the component only while it (or an ancestor path) has focus |
| VALUE_CHANGED | `onChange` on TextInput, MultiLineTextInput, TextInputForType, Button, RockerButton, TabButtonComponent, ListBox, DropdownListBox, TextInputWithIncrDecrButtons, ScrollBars, ImageManipulationControls (`HasValue.UTIL.bindConsumerAsOnValueChangedHandler`) | component actions (a Button's action is a VALUE_CHANGED handler) |
| VALUE_CHANGED (from children) | `FormBinder.onChange` on the **form panel** (ancestor) | form-level change handling: needs delivery through ancestors |
| VIEW_OFFSET_CHANGED | `HasViewPortWindow` (`newOnChangedHandler`) | viewport scroll sync |
| TABLE_ROW_SELECTED / TABLE_CELL_SELECTED | `TableComponent` (`rowSelectedHandler`, `cellSelectedHandler`) | table selection callbacks |
| REQUEST_SCROLL_TO_VIEW | `WindowViewComponent.ScrollToViewHandler` in `ScrollableComponent`, `ScrollableComponent2`, `WindowFrame`, `WindowFrameFocusable` chains (plus a bus-level `FindScrollToViewHandler` in `TUIApp`) | scroll a focused/requested component into view. **Focus side effect:** focusing inside a scrollable can scroll it (see comment in `MouseEventOrchestrator`) |
| REQUEST_FOCUS | `ScrollableComponent` adds its **own** request-focus handler ("not a standard FocusManager") | focus requests resolved inside the scrollable |
| any (forwarded) | `ScrollableComponent2`: if the footer (e.g. search box) has focus and cannot handle an event but the viewed component can, it forwards the event **and then raises `RequestFocusEvent` for that component** | *event-driven focus*: typing moves focus to the component that consumes it |
| any | `ScrollableComponent` prepends each child focusable directly into its own chain | a fourth focus-manager flavour, besides the three base classes in §3.1 |
| any | `customExtendedEventHandler` on `DefaultFocusManager` (`TextInputWithIncrDecrButtons`, `MovingPicturesViewer`, `AbstractWrappedComponentWithFocusManager`) | extra per-manager handling |
| KEYPRESS (modal) | `ModalEventHandler` prepends itself to its parent container (bus or component), inner chain + keyboard sinkhole | modal dialogs swallow all keys |

On the bus (not via focus tree): idle/terminate handlers, canvas refresh, cursor show/hide,
resize, `FindScrollToViewHandler`, mouse orchestrator, component inspector,
`SearchFunctionForScrollableComponent`'s title updater (added to the bus at runtime).

---

## 3. How components get focus managers

### 3.1 Three base classes own a `DefaultFocusManager`

| Class | Shape | Nav default |
|---|---|---|
| `CompositeWithFocusManager` | *is* a `Composite`, delegates focus to an inner DFM | `NAVKEYS_LAST` |
| `ComponentWithFocusManager` | *wraps* a `Composite`, delegates focus to an inner DFM | `NAVKEYS_FIRST` |
| `AbstractWrappedComponentWithFocusManager` | wraps any component; `asFocusable()` returns the inner DFM | `NAVKEYS_FIRST` |

Subclasses with their own manager include `ButtonBar`, `DropdownListBox`,
`TextInputWithIncrDecrButtons`, `Menu`, `MenuBar`, `MovingPicturesViewer`,
`ScrollableComponent(2)`, `SearchFunctionForScrollableComponent`,
`WindowFrameFocusable`, `LinuxProcessViewer`.

The wrappers call `DefaultFocusManager.setActual(wrapper)`, so children added to
the inner DFM record the **wrapper** as their parent, not the DFM. This keeps
the tree consistent as long as every wrapper does it (see §4.2). The code's own
comment: *"this feels all very hacky — a weakness in my model I suspect"*.

### 3.2 Keeping the focus tree in sync — `BehaviourForCompositeAndFocusManager`

When a child component is added to a composite that owns a manager, the child's
focusables are collected and added to the manager:
`FocusManager.UTIL.focusablesFromComponent(child)` descends the component tree
and **stops at the first Focusable/ToFocusable on each branch**. So
nested managers become a single entry, and plain nested composites are
flattened into the parent's list.

This is a **snapshot at add time**. If something is later added deeper inside a
child that is *not* itself a manager, the parent manager never hears about it.

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
1. **Two kinds of container, one mechanism.** Nested focus containers come in two kinds
   that need opposite rules:
   - *Grouping container* (form section, panel of fields): children are separate tab
     stops. TAB entering forwards should start at the first child, entering backwards at the last.
   - *Composite widget* (ListBox, DropdownListBox, menu, tab strip): the widget is **one**
     tab stop, arrow keys move inside it, and re-entry **should** resume the remembered child.

   A `DefaultFocusManager` can't tell which kind it is. Re-entry always resumes the
   remembered `current` (widget behaviour), but if the manager's nav keys are TAB it also
   makes each child a tab stop (grouping behaviour). The result follows neither rule:
   first lap `A B C D`, then B is unreachable. Managers that navigate with other keys
   (`DropdownListBox` uses UP/DOWN) already behave correctly as widgets. The manager is
   also never told the direction focus arrived from, which a grouping container needs.
2. **Nav-style mismatch across levels.** An outer `NAVKEYS_FIRST` manager takes
   TAB before the inner group sees it, so the inner group can't be traversed. Nav
   style is chosen per class, so how TAB behaves depends on which components you
   happened to compose.
3. **Cycling inference** only works if `setParentFocusManager` is called in the
   right order. An explicit `setCycling(true)` on a nested manager traps TAB inside it.

### 4.2 Structural causes

- **Two trees kept in sync by hand.** The component tree (layout, hit-testing,
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
  container's focusables *before* `setActual(this)`, so those children's parent is the inner DFM
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
- *container*: a component that holds other focusable components and manages focus among them
  (a form panel, a button bar, a dropdown, a window). In code: a `FocusManager`.
- *nav keys*: keys that move focus rather than being typed, e.g. TAB / SHIFT-TAB; each container
  can have its own (a dropdown uses UP / DOWN).

1. **One owner of focus.** *(done - step 4, in this form:)*
   "Who has focus?" used to be answered by combining many separate pieces of state: every container
   remembers its own "current child", and every component keeps its own `hasFocus()` flag. These
   could disagree - e.g. after a click, a dropdown's button still reported `hasFocus()==true` while
   an image viewer actually had focus (§5).
   Now the ROOT focus manager keeps **one** field, "the focused component"
   (`DefaultFocusManager.getFocusedComponent()`), updated by every focus move - `moveFocus` and the
   old routing alike - and `FocusManager.UTIL.findCurrentFocus(root)` answers from it. When focus
   moves, `moveFocus` also checks the previous owner really let go: if it still claims focus (missed
   its `unFocus`), it is switched off - so exactly one component claims focus.
   Still there, but no longer the source of truth: components' `hasFocus()` flags (display state:
   hilite) and containers' "current child" (a widget's memory of its last child). Removing them
   entirely - `hasFocus()` meaning "am I the owner?" - would touch every focusable component; only
   worth doing if they cause trouble again.

2. **Who gets a key first.** *(done - step 2, `FocusNavigator`)*
   When a key arrives:
   1. If it is not a nav key of any container around the focused component, it is ordinary input
      and goes where it always did.
   2. If the focused component wants it anyway, it gets it - e.g. a multi-line editor that inserts
      TAB characters. *(Planned: such an editor releases TAB again after Esc.)*
   3. Otherwise the containers around the focused component are asked, innermost first, where
      focus should go (point 3).

3. **Containers answer two questions; they decide, their parent doesn't.** *(done - steps 1 and 2)*
   - `enter(direction)`: "focus is arriving in you, moving forward (TAB) or backward (SHIFT-TAB) -
     which of your components should get it?" Returns that component, or null for "nothing in here
     can take focus, skip me" (empty, disabled, or not a TAB stop).
   - `advance(focused, direction)`: "a nav key was pressed while focus is inside you - where next?"
     Answers MOVED(component), DONE ("no more in that direction - ask my parent") or STAY ("handled,
     focus stays").
   How a container answers depends on its *policy*:
   - **GROUP** - each child is its own stop (a form section: TAB visits every field). Entered
     forward it starts at its first child, backward at its last. The outermost container wraps
     around instead of answering DONE.
   - **WIDGET** - the whole container is one stop (dropdown, list box, tab strip). Entering it
     returns to the child it had last time. Its own arrow keys move inside it. *(Step 2: widgets
     still handle their own keys the old way; only entering / leaving them uses these answers.)*
   - **TRAP** - focus cannot leave (modal dialog): navigation wraps around inside.
   A container - or a single component - can also be marked *not a TAB stop* (`setTabStop(false)`):
   TAB skips it, clicks and hot keys still work - e.g. the scroll buttons under a scrollable panel,
   and a window's title bar and resize corner (so TAB goes straight to the window's content).
   Example, form `A, [B, C], D` where `[B, C]` is a GROUP: TAB goes A B C D A B C D...; if `[B, C]`
   were a WIDGET, TAB would go A B D A B D... and arrow keys move between B and C.

4. **One way to move focus.** *(done for clicks and TAB - step 3, `FocusManager.UTIL.moveFocus`)*
   `moveFocus` changes focus and sends the existing notifications: `unFocus(new)` to what loses focus,
   then `focus(old)` to what gains it, along the path (hilite, bring-to-front and the dropdown / menu
   hand-off depend on these, §4.4). It goes through each container's `setChildAsCurrentFocus`, so
   components that customise that method keep working. It works in two passes: containers that don't
   have focus yet first just record which child to use (deepest first, no notifications); then focus
   is passed down top-down and arrives straight at the target. Without the first pass a container
   receiving focus handed it to its remembered child (or its first child) first - a flicker, and in
   `ImageSplitterTest` a window wrongly brought to front.
   *(Still moved the old way: widgets' own arrow keys, hot keys, and code calling
   `setChildAsCurrentFocus` directly.)*
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
2. Keep today's path and order: from the root down through the containers, focused path
   first, then other children using each container's current first-claimer or all-claimers rule
   (pinned by characterisation tests). The only change: containers are discovered on demand.
3. **No "targeted" delivery straight to the event's source.** Ancestors listen to their
   children's events: `FormBinder.onChange` registers on the *form panel*, an ancestor of the
   inputs, so skipping ancestors would break form-level change handling.

**C. Shared: on-demand discovery.** Neither job keeps a stored list; both walk the
component tree when needed (nav keys, clicks, drag start/end, broadcasts). Nothing goes
stale, and `rebuildFocusManager()` calls become unnecessary.

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
| Broadcast rule | **first claimer everywhere** (as `NAVKEYS_FIRST` does today; also how the bus's main chain works). Behaviour change at the root, which today (`NAVKEYS_LAST`) delivers to all claimers. Pinned by `FocusProtocolCharacterisationTest`, to be flipped deliberately |
| Focused component hidden/disabled | move focus to the next focusable (else previous, else none); triggered via `setEnabled(boolean, EventBus)` / hide |

This follows the same approach as browsers (tab order derived from the DOM, roving
tabindex for composite widgets) and most modern TUI toolkits.

### 4.4 Constraints any redesign must keep

- **Navigation keys are configurable.** Never hardcode TAB/SHIFT-TAB; use each
  container's configured nav keys (`DropdownListBox` uses UP/DOWN, for example).
- **The child decides.** A parent offers the nav key to its current child first. The
  child answers "moved internally" or "done", and the parent moves on only when the
  child is done. Entry carries a direction, so a grouping child can pick its first or last
  child and a widget can resume its remembered one. A child with nothing focusable
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
  new path, **top-down, containers included**. That is how `WindowFrameFocusable` hilites/unhilites
  its border, `AbstractWrappedComponentWithFocusManager` does `bringToFront`, and
  `DefaultFocusableBehaviour` recolours components. Keep the within-node order too (e.g.
  `WindowFrameFocusable.focus` passes focus down first, then hilites itself).
- **The child decides; the parent never inspects the policy.** `advance` answers MOVED(target),
  DONE (bubble up) or STAY (handled, no move, e.g. DOWN at the end of a list). `moveFocus` *tells*
  containers "focus is now inside you at child X" (cf. `childHasCurrentFocus`), and each
  container decides whether to remember it.
- **A popup belongs to its owner for navigation.** A dropdown's list or a menu's popup may be
  attached elsewhere in the component tree (root panel overlay); leaving it with a nav key must
  behave as leaving the owner in that direction.
- **Existing modal trap:** `ModalEventHandler` is prepended to the bus and swallows all keys
  (keyboard sinkhole). It is separate from focus managers and must keep working.
- **Remembered state belongs to the widget** (for example, a ListBox's selected index), not to a
  stored list in a manager. If the remembered child is gone, disabled or hidden, fall back
  to the first child.
- **Visibility:** the focus code currently has no notion of hidden components.
  Untested whether hidden components (for example, inside a collapsed `CollapsiblePanel`) stay tab stops.
- **Wrap-around may re-enter the starting child.** When a wrapping container (root or
  trap) comes back round to the child that just reported "done", it must re-enter that child
  from its first/last leaf instead of giving up. Otherwise a root whose only child is a group dead-ends,
  which is today's bug, and the first prototype had it too.
- **Open question:** when the *focused* component becomes hidden or disabled, who moves focus
  off it, and when? Currently nothing does; the next nav key just restarts from the top.

### 4.5 Prototype result (Sep 2026)

A standalone prototype of §4.3 and §4.4 ([docs/prototype/ProtoFocus.java](prototype/ProtoFocus.java), outside `src` so it is not built; one focus root, children discovered
on demand, `enter(dir)`/`advance(dir)`, four container kinds: transparent panel, group,
widget, trap) passes all 20 scenarios:

- grouping: `A,[B,C],D` gives `A B C D A B C D…`, and SHIFT-TAB gives `D C B A…`
- widget: one tab stop, arrows move inside, re-entry resumes the remembered child (also after a click)
- a single nested group as the root's only child gives no dead end
- plain panels are transparent (flattened); disabled, hidden and empty containers are skipped;
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
then merge or remove only where the remaining behaviour is a focus *policy* (GROUP / WIDGET / TRAP)
that a plain container can declare. Do this class by class, not in the engine commit.

---

## 5. Wart list (event/focus area)

| Where | Issue |
|---|---|
| `events/EventHandlerContainer.java:22-31` | `Simple.prependEventHandler` appends and `appendEventHandler` prepends (swapped) |
| `events/ChainedEventHandler.java:211-215` | removing a pending prepend never works (`!contains` inside a `contains` check) |
| `events/ChainedEventHandler.java:143` | public `ChainedEventHandler(EventHandler)` ctor leaves strategy null → NPE on first event |
| `events/ChainedEventHandler.java:403` | `TRACE_AND_LOG_EVENT_HANDLERS = true` logs every handler test for every keypress (likely cause of the 200 MB `dtbTerminal.log`) |
| `events/EventBusImpl.java:186-195` | full queue: `offer` result ignored, event silently dropped |
| `events/ConditionalEventHandler.java:133` | `AbstractDelegate.setName` ignores its argument |
| `events/focus/DefaultFocusManager.java:161-163` | `setChildAsCurrentFocus(null)` → NPE (`focusable.getParentFocusManager()` before null check) |
| `events/focus/DefaultFocusManager.java:50,65` | field default says `NAVKEYS_LAST`, constructor overrides to `NAVKEYS_FIRST` |
| `events/focus/DefaultFocusManager.java` | **removed (step 4):** dead `getPrevOLD` / `getNextOLD` |
| `components/BehaviourForCompositeAndFocusManager.java:169-174` | insert index uses a *map* index as a *children-list* index → wrong tab order when inserting before existing children |
| `controlcodes/ControlCode.java` | several `DISABLE_*` mouse codes have an extra digit (`[?10021l` should be `[?1002l`, etc.) |
| `components/ButtonBar.java:77-80` | **fixed (Sep 2026):** `setNextKeyStroke` called `setPrevKeyStroke`, so "next" stayed TAB and the prev key was overwritten (e.g. `TabPanelTest`'s Ctrl-Left/Ctrl-Right never worked) |
| `components/tab/TabPanel.java` | **fixed (Sep 2026):** tab strip navigated with TAB + `setCycling(true)` (a focus trap), and activating a tab always moved focus into its content, so strip navigation stopped after one step. Now: on the strip, Left/Right (wrapping) show tabs and focus **stays on the strip**; TAB goes strip → content → `<-` `->`; click, tab hot key and the prev/next shortcuts (`setPrevKeyStroke`/`setNextKeyStroke`) show the tab **and** focus its content. The shortcuts work from anywhere inside the panel but sit last in its chain (`customExtendedEventHandler`), so the content gets the key first |
| `components/input/DropdownListBox.java` | **fixed, interim (Sep 2026):** the open list is attached to the root panel, i.e. at the *end* of the root's focus order, not inside the dropdown. TAB from the list wrapped to the first focusable, and re-entry then resumed the dropdown: a loop (dropdown ↔ first focusable), so nothing after a dropdown was reachable by TAB (e.g. a form's Save button). SHIFT-TAB went to the root's last focusable. Now TAB/SHIFT-TAB in the list hand focus back to the dropdown and re-raise the key. Proper rule for the redesign: **a popup belongs to its owner for navigation** |
| `components/input/DropdownListBox.java` | **fixed (Oct 2026):** clicking elsewhere while the list was open left it open. The dropdown already got its `unFocus` when focus moved into the list, so a later focus move only unfocused the list. In windowed apps the list then hid *behind* the window (bring-to-front re-send) but stayed open and in the tab order. Now `ListBox.onUnFocus` (new protected hook) lets the dropdown close the list once the move has finished, if neither the list nor the dropdown has focus |
| `components/input/DropdownListBox.java` | Enter/Esc in the list commit the selection and return keys to the dropdown, but the list stays **open** until focus moves elsewhere (`hideListBox()` commented out in `onSelectedItemHandler`). Esc commits instead of cancelling |
| `components/modal/ModalEventHandler.java` | modal dialogs only trap **keys**; mouse clicks behind the dialog still go through and move focus out of it (seen in `ImageSplitterTest`'s Options dialog). Owner note: modals are also hard to create. Both for later |
| focus flags (distributed state) | **observed (Oct 2026):** two components reporting `hasFocus()==true` at once. In `ImageSplitterTest`, after clicking from a dropdown's open list (inside the Options dialog) onto the image, the dropdown's button still had `hasFocus()==true` while the viewer had focus. Concrete evidence for a single focus owner (§4.3 A). `DropdownListBox` now asks the root focus manager instead of trusting flags |
| `events/focus/FocusManager.java` (`buildRequestFocusEventHandler`) | **keep (Oct 2026):** each click re-sends `setFocus(true)` down the currently focused chain before moving focus. Looks redundant but re-asserts hilite / bring-to-front of ancestors that stay focused; see §4.4 |
| `events/focus/DefaultFocusManager.java` (`getCurrentOrSet`) | **fixed (step 3):** a container with no current child picked child 0 and focused it - on the first click, and whenever its current child had been removed (e.g. a dropdown's list closing), for **any** event reaching it. In `ImageSplitterTest` that brought the wrong window to front. Now clicks / TAB go through `moveFocus` (records the path first), and non-key events never pick a child. Key events in the old routing still may |
| `components/scroll/ScrollableComponent.java` (old, used by `TextBoxTest`'s tree view) | handles focus its own non-standard way; e.g. SHIFT-TAB out of it into a GROUP lands on that group's first field instead of its last. Candidate for the ScrollableComponent consolidation (§4.7), not worth fixing on its own |

---

## 6. Characterisation tests (focus safety net)

Tests that record what the event/focus system does **today**, so the focus redesign cannot change
behaviour silently. A failure means "behaviour changed": decide whether the new behaviour is wanted,
then update the expectation deliberately. Tests named `..._today_...` pin known bugs; they are
expected to fail when the redesign fixes them.

| Test class | Covers |
|---|---|
| `events/focus/FocusCharacterisationTest` | nested TAB / SHIFT-TAB order (real components), button `onChange` via the focus tree |
| `events/focus/FocusProtocolCharacterisationTest` | exact `focus` / `unFocus` / `setFocus` calls and order, click flicker, no initial focus, disabled skip, root dead end, non-key delivery rules |
| `components/FocusSideEffectsCharacterisationTest` | frame hilite on click and TAB (frame TAB order: title, content, resize anchor), TextInput recolour, scroll-to-view request |
| `components/input/form/FormChangeCharacterisationTest` | typing -> data -> field `onChange` -> form `onChange` through the form panel |
| `components/input/DropdownListBoxCharacterisationTest` | open on focus, hand-off into the list, TAB / SHIFT-TAB / ENTER in the list, click elsewhere closes |
| `components/tab/TabPanelCharacterisationTest` | click / arrows / TAB / shortcuts |
| `components/modal/ModalEventHandlerCharacterisationTest` | modal key sinkhole |

`supportfortesting/SyncEventLoop` is a synchronous stand-in for the event bus: events go to one root
handler (bus-level handlers are not involved), `then(..)` callbacks run after each event, `asynch`
tasks run when the queue is empty, repeating timers are ignored.

Not covered yet: menus (popups on a live canvas), `ScrollableComponent2` footer focus forwarding,
actual scrolling, hotkeys, table selection events, bring-to-front.
