# Building a component

This guide is for writing your own component: a control, a panel of controls, or something that
just draws. It covers the few choices you make and the conventions that let your component work
with the rest of the library. The reasons behind the design are in [ARCHITECTURE.md](ARCHITECTURE.md);
you don't need them to follow this.

## 1. Pick one of three shapes

Answer one question: **where does focus go?**

| Your component | Focus | Build it as | Library examples |
|---|---|---|---|
| a layout of other components (a form, a page) | to the fields inside it | a `Composite` - nothing else to do | any app's form panel |
| one control (a button, a check box, a tree that draws its own rows) | to the component itself: it is one TAB stop | hold a `LeafFocusBehaviour` | `CheckBoxInput`, `TreeView`, `TableComponent` |
| a control made of other controls (a button bar, a spinner with + and - buttons) | to the controls inside it, moving the way you say | extend `ComponentWithFocus<Panel, Self>` | `ButtonBar`, `NavButtonsPanel`, `TextInputWithIncrDecrButtons` |

A component that only draws (a chart, a label) takes no focus: it is none of the three.
Extend `BoundedComponent` and draw (section 4).

You never register a component with a focus manager. Focus is worked out from the component tree
whenever it is needed, so adding, removing or hiding a component later needs no extra call.

## 2. A layout: `Composite`

```java
Composite form = new Composite(color);
form.add(0, 0, new TextComponent("Name:"));
form.add(0, 8, name);              // a TextInput
form.add(2, 8, save);              // a Button
```

TAB goes name → save, the order they were added. That's all.

## 3. One control: hold a `LeafFocusBehaviour`

The component holds the behaviour and hands it out. Handing it out is what makes the component
focusable. Then:

- it is hilited while focused;
- it is scrolled into view when it gets focus;
- it gets keys first while focused.

```java
public class CheckBoxInput extends BoundedComponent<CheckBoxInput> implements ToFocusable, AsKeyboardHandler {

    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .showsCursorAt(new Position.Simple(0, 2))
            .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(
                    new KeyInputEventHandler((te) -> toggle(te.getEventBus()), Keys.ENTER_KEYSTROKE, Keys.SPACE_KEYSTROKE)));

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }
    ...
}
```

The other settings, all optional:

```
look(Look.NONE)              you show focus yourself (e.g. hilite only the selected row)
showOnFocus(() -> rect)      scroll this part into view on focus, not the whole component
showsCursorAt(pos)           put the terminal cursor here while focused
handleKeys(..)               its keys (section 5)
handleEvents(..)             its other events, e.g. its own "value changed" for onChange (section 6)
whenFocusArrives / whenFocusLeaves
```

## 4. Drawing

A component draws into a segments buffer: rows of text with their colours. Positions are relative
to the component's top-left corner, starting at 0. The usual way is to let a `TextComponent` do the
work and copy its segments:

```java
@Override
protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
    textRenderer.setText(buildRenderText());
    mySegments.clearAndMerge(textRenderer.getSegmentsBuffer().getSegments());
}
```

`draw` runs only when the component is **dirty**. After changing what it shows:

1. call `markAsDirty()`;
2. raise `new RequestUpdateCanvasEvent(this)` on the bus, so the screen is redrawn.

**Text rule:** one character is one screen column. Don't put a tab or any other control
character (code 27 or below) into text you draw. Such characters are stripped when drawn, so the
line comes out shorter than you meant. `MultiLineTextInput` turns TAB into spaces for this
reason. Wide characters (CJK, emoji) aren't handled; the library assumes width 1.

A component with parts inside it (as `CheckBoxInput` holds its `TextComponent`) is a
`ParentContainer`. It sets itself as each part's parent and answers `getChildren()` and
`getPositionRelativeToParent(child)`. Mouse hit-testing and focus rely on this tree, so keep it
accurate.

## 5. Keys

A key goes to the focused component first. If that component doesn't use it, the key goes to the
component's parent, then that parent's parent, and so on: it **bubbles up**, as in a web browser.
Only when nobody used it do TAB, the arrows of a `singleTabStop` group, or hot keys act.

There are two ways to say what keys a component uses:

- **`onKey`:** implement `KeyboardHandler` and return `true` for the keys you used. This is the
  simplest way when the answer depends on state (e.g. "DOWN opens the list when it's closed").
- **An event handler, adapted:** `AsKeyboardHandler.UTIL.toKeyboardHandler(new KeyInputEventHandler(action, keys...))`.
  The key test is written once, and the library can ask "do you want this key?" without acting.

Pass either to `handleKeys(..)` on the behaviour.

### Hot key or local key?

| Want | Do | Reaches the component |
|---|---|---|
| a **hot key**: works from anywhere on the screen | `button.setHotKey(Keys.CTRL_T_KEYSTROKE)` (or implement `HasHotKey`) | always - focus jumps to it |
| a **local key**: works only while focus is inside your component | `focus().handleKeys(..)` on the parent component | only while focus is inside it |

Hot keys are global, so use only keys nobody else needs: CTRL or function keys. A plain key such
as UP made a hot key takes the arrows away from every text box on the screen. Example:
`SparklineTest` handles UP and DOWN as local keys of its button bar.

## 6. Values and `onChange`

A control with a value implements `HasValue<V>`. When the user changes the value:

```java
setValue(newValue);
bus.raiseEvent(new HasValue.ValueChangedEvent<>(this));
bus.raiseEvent(new RequestUpdateCanvasEvent(this));
```

The event travels down through the focus tree to the component that raised it. Pass it to your
`onChange` listeners with `handleEvents(..)`. `OnChangeListeners` holds them: any number, each
`onChange` call adds one and hands back a `Registration` that takes it out again.

```java
private final OnChangeListeners<Boolean> changeListeners = new OnChangeListeners<Boolean>(this);

public Registration onChange(EventHandler handler) {
    return changeListeners.add(handler);
}

... LeafFocusBehaviour.of(this)
        .handleEvents(new ConditionalEventHandler.Impl(
                (te) -> changeListeners.canHandle(te) != null,      // only while someone listens
                (te) -> changeListeners.handle(te)));
```

A component whose events reach its own handler chain instead adds the listeners there:
`return changeListeners.add(getChainedEventHandler(), handler);`.

## 7. A control made of controls: extend `ComponentWithFocus`

It holds the panel that lays out the parts (e.g. a `Composite` or a `HorizontalPanel`) and their
focus. Border, title, colours and hilite are the panel's, and they are forwarded for you.

```java
public class NavButtonsPanel extends ComponentWithFocus<Composite, NavButtonsPanel> {

    public NavButtonsPanel(Color color, ...) {
        super(color, new Composite(color));
        getPanel().add(..);                          // the buttons

        focus().singleTabStop()
               .arrowsMoveInside(ArrowKeys.UP_DOWN)
               .enterAt(Entry.FIRST_OR_LAST);
    }
}
```

By default every part is its own TAB stop; then you set nothing. To change how focus moves, use
`focus()`:

```
singleTabStop()                      TAB treats the whole component as one stop ...
arrowsMoveInside(ArrowKeys.UP_DOWN)  ... and these keys move between its parts
wrapAround()                         past the last part goes back to the first
enterAt(Entry.FIRST_OR_LAST)         where focus lands when it arrives
enterAt(Entry.LAST_FOCUSED)            ... or back where it was
enterAt(() -> part)                    ... or a part you choose (a radio group: the chosen option)
skippedByTab()                       TAB passes it by (mouse and hot keys still reach it)
keepsFocusInside()                   TAB never leaves it (a dialog)
takeFocusItselfWhenNoChildCan()      nothing focusable inside: it is the stop itself
handleKeys(..) / handleEvents(..)    its own keys (those its parts didn't use) and events
whenChildFocused / whenFocusArrives / whenFocusLeaves
```

If your component can't extend `ComponentWithFocus` (for example its panel has no border or title),
hold a `ParentFocusBehaviour.of(this)` and hand it out with `asFocusable()` and
`asKeyboardHandler()`, the same way as the leaf behaviour in section 3. `RadioGroup` does this.

## 8. Wrapping a component

A wrapper adds something around a component, e.g. making it clickable. It implements `ToFocusable`.

- If the wrapper should be a TAB stop itself, `asFocusable()` returns its focusable.
- If the wrapped component has focusable parts of its own, `asFocusable()` returns `null`. Focus
  then goes straight to those parts, as if the wrapper weren't there.

Don't build a focus manager inside a wrapper: it would be a second focus tree that disagrees with
the real one.

## 9. Testing it

The focus tests show the pattern; `RadioGroupTest` is a short one to copy. Build a small tree on a
`CompositeWithFocusManager` root, deliver events with `SyncEventLoop`, and check what has focus and
what your listeners saw:

```java
SyncEventLoop bus = new SyncEventLoop();
CompositeWithFocusManager root = new CompositeWithFocusManager();
root.add(myComponent);
CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
bus.setRoot(root);
bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
```

To try it on screen, add it to an example under `mizzen.examples` and run that example's `main`.

## 10. Checklist

- [ ] One of the three shapes: `Composite`, a held `LeafFocusBehaviour`, or `ComponentWithFocus`.
- [ ] Parts have their parent set, and `getChildren()` lists them.
- [ ] After a change: `markAsDirty()` and `RequestUpdateCanvasEvent`.
- [ ] Text holds no tabs or control characters.
- [ ] Hot keys only for keys nobody else needs; otherwise local keys via `handleKeys`.
- [ ] A value change raises `ValueChangedEvent`, and `onChange` is wired through `handleEvents`.
- [ ] A test with `SyncEventLoop`.
