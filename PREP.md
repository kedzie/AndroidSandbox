# Staff Engineer Android Interview Prep

## Todo

### High Priority (likely interview topics)

- [ ] **Custom Layout deep dive**
  - `Layout` composable — constraint propagation, measure/place pipeline
  - Why you cannot measure a child twice (`IllegalStateException`)
  - `Intrinsics` — `IntrinsicSize.Min/Max`, when the parent needs to know child size before measuring
  - Real example: a custom chat bubble layout or split-pane that uses raw constraints

- [ ] **LookaheadScope / `approachLayout`**
  - Two-pass system: lookahead pass (final bounds) vs approach pass (animated current bounds)
  - Real example: item that animates from list position to fullscreen without SharedTransitionLayout
  - How `SharedTransitionLayout` uses LookaheadScope under the hood

- [ ] **SubcomposeLayout**
  - Why it exists: measuring content before knowing how much space you have
  - Real example: a `Scaffold`-like component where FAB position depends on SnackbarHost height
  - Performance cost: subcomposition is expensive, when NOT to use it

- [ ] **Compose profiling in Android Studio**
  - Layout Inspector — recomposition counts, highlight recomposing nodes
  - Composition Tracing — see composable names in system trace (needs `tracing` dependency)
  - Perfetto / CPU profiler — identifying jank, dropped frames
  - `recomposeHighlighter()` modifier for development
  - Reading the skipped/recomposed/composed counts in Layout Inspector

- [ ] **`animateItem()` in LazyColumn**
  - `key` parameter on `items()` — why it matters for state preservation and animation
  - `Modifier.animateItem()` — replaces old `animateItemPlacement()`
  - Bug: state (expanded card, TextField text) jumping to wrong item without `key`
  - Paged list keys with `PagingData` — `peek()` to get key without triggering load

- [ ] **`@Stable` vs `@Immutable`**
  - `@Immutable` — compiler promise that all public fields are deeply immutable, never change
  - `@Stable` — fields can change but reads are tracked (like a `State<T>`)
  - Why plain `List<T>` is unstable and causes composables to never skip
  - Fix patterns: `@Immutable data class`, `ImmutableList` (kotlinx.collections.immutable), wrapper class
  - How to verify with compiler reports: `freeCompilerArgs += ["-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=..."]`

- [ ] **Semantics / Accessibility**
  - `Modifier.semantics { }` — contentDescription, role, onClick, stateDescription
  - Merged vs unmerged semantics tree — `mergeDescendants = true`
  - `clearAndSetSemantics { }` — replace child semantics entirely
  - Custom Canvas components need manual semantics (chart, custom drawn button)
  - Testing with semantics: `composeTestRule.onNodeWithContentDescription(...)`

- [ ] **`rememberSharedContentState` — when to remember vs not**
  - Key stability: if key is computed from unstable state, state object recreates each recomposition breaking the match
  - `remember(key) { rememberSharedContentState(key) }` — when and why
  - Why the same key must resolve to logically identical content

- [ ] **`rememberUpdatedState`**
  - The stale lambda closure bug in long-running `LaunchedEffect`
  - Any `LaunchedEffect(Unit)` that captures a callback parameter needs this
  - Pattern: `val current by rememberUpdatedState(callback)`

- [ ] **`DisposableEffect` and `SideEffect`**
  - `DisposableEffect(key)` — setup + guaranteed cleanup via `onDispose {}`
  - Use cases: lifecycle observers, sensor registration, broadcast receivers
  - `SideEffect` — runs after every successful recomposition, sync Compose state to non-Compose world
  - `LaunchedEffect` vs `DisposableEffect` decision rule

---

### Medium Priority

- [ ] **Shared element nav-tab → fullscreen camera transition**
  - Camera tab icon morphs into full PreviewView using `sharedBounds`
  - `OverlayClip` with `MorphOverlayShape` driven by `transition.animateFloat`
  - `PreviewView.ImplementationMode.COMPATIBLE` (TextureView) required for clipping
  - `skipToLookaheadSize()` on destination content

- [ ] **FAB / TopAppBar driven by current NavEntry**
  - Inspecting `backStack.lastOrNull()` in global Scaffold slots
  - Per-screen Scaffold pattern as alternative

- [ ] **`snapshotFlow`**
  - Converts Compose `State` reads into a cold `Flow`
  - Use case: observing scroll position and emitting only when crossing a threshold
  - vs `derivedStateOf` — snapshotFlow for crossing into coroutine world, derivedStateOf for staying in Compose

- [ ] **Predictive back + shared elements**
  - `predictivePopTransitionSpec` in NavDisplay
  - Seekable transitions require `spring` or `tween`, not custom easing
  - Two-pane layout collapsing during predictive back swipe

- [ ] **Vector animations / AnimatedVectorDrawable in Compose**
  - Icon morphing (play → pause, hamburger → back arrow)
  - `AnimatedImageVector` and `rememberAnimatedVectorPainter`
  - `Transition.animateFloat` driving custom path morph

---

### Reference (already strong, just review)

- [x] Shared element transitions — `sharedBounds` vs `sharedElement`, key matching, scope issues
- [x] Phase isolation — graphicsLayer, drawBehind, state reads in draw lambda
- [x] `derivedStateOf` vs `snapshotFlow` vs raw state
- [x] Strong skipping mode and lambda memoization
- [x] Nav3 + ListDetailSceneStrategy adaptive layout
- [x] `stateIn` / `WhileSubscribed` / `SharingStarted`
- [x] Coroutine scope choice — `viewModelScope` vs `rememberCoroutineScope` vs `LaunchedEffect`
- [x] `Channel` vs `SharedFlow` vs `StateFlow` for VM→UI events
- [x] SSE streaming → Room → ViewModel observation pattern
- [x] CameraX + `AndroidView` interop
- [x] `RuntimeShader` / AGSL + `RenderEffect` via `graphicsLayer`
- [x] `Morph` / `RoundedPolygon` shape morphing
- [x] `DrawScope` — `Brush`, `ShaderBrush`, `drawIntoCanvas`, `BlendMode`
- [x] Modifier order — alpha/background, graphicsLayer wrapping
- [x] `TopAppBar` scroll behavior + parallax hero image
- [x] `movableContentOf` + `sharedElementWithCallerManagedVisibility`
- [x] FCM push → Service → Repository → Room → ViewModel
- [x] `rememberUpdatedState` stale closure pattern
- [x] Navigation drawer — `ModalNavigationDrawer`, gesture conflict with predictive back
