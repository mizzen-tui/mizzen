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
- **Wrapper vs inner manager (fragile; no failure reproduced).** Component → focusable
  identity is stable, because every `asFocusable()` checked (~18) caches its instance lazily.
  The fragile part is the wrappers (§3.1): the tree is only consistent because each wrapper
  calls `setActual(wrapper)`, so children point at the wrapper and not at the inner
  DFM. A wrapper that skips this, or code that adds `wrapper.getFocusManager()`
  directly, gives the same node two identities, and path finding then depends on which
  direction it searches. The disabled `skipProxiedManagers` workaround in
  `verifyPathsToFocusables` suggests this has failed before.
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

### 4.3 Direction (to decide)

The requirement: *any composition of components must give consistent TAB order
and click-to-focus, with no failures*. That points to:

- **One focus authority per app** (or per window/modal layer). It computes
  traversal order on demand by walking the **component tree** in document order,
  filtering to focusable + enabled + visible. There is no stored focus tree, so
  nothing can drift.
- **Focus state in one place:** a single "focused component" reference. Ancestors
  can ask "is focus within me?" instead of holding their own flags.
- **Each container declares its kind** instead of it being implied by nav keys
  and class defaults:
  - *grouping* (default for plain panels): children join the surrounding tab order;
    entry is direction-aware (first child forwards, last child backwards).
  - *widget* (ListBox, DropdownListBox, menu, tab strip, and a ButtonBar if wanted): one
    tab stop, **remembers its last child**, internal navigation with its own keys (arrows).
  - optionally *trap* (modal dialog): TAB cycles inside and never leaves.

  Not "flatten everything": remembering the last child is correct for widgets and must be kept.
- **`canHandle` becomes a pure query.**

This is the approach browsers take (tab order derived from the DOM) and most
modern TUI toolkits too. Migrating needs a compatibility plan for the ~15
classes in §3.1.

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

A standalone prototype of §4.3 and §4.4 (kept outside the repo; one focus root, children discovered
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

Not covered by the prototype: real mouse and drag plumbing, the `REQUEST_FOCUS` event path,
hotkeys, non-key events routed to non-focused children, canvas refresh, and fitting it
into the existing `FocusManager` API and wrapper classes.

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
| `events/focus/DefaultFocusManager.java:188,226` | dead `getPrevOLD` / `getNextOLD` |
| `components/BehaviourForCompositeAndFocusManager.java:169-174` | insert index uses a *map* index as a *children-list* index → wrong tab order when inserting before existing children |
| `controlcodes/ControlCode.java` | several `DISABLE_*` mouse codes have an extra digit (`[?10021l` should be `[?1002l`, etc.) |
