# Lista Zadań i Roadmapa (TODO.md)

> **Zadania projektowe, planowane optymalizacje oraz wytyczne architektoniczne dla silnika Java Voxel 3D.**

---

## 📌 Zadania Priorytetowe (Z `TODO.readme`)

### 1. Priorytetyzacja generowania chunków według odległości od gracza
- [x] **Zrealizowano:**
  - Dodano funkcję oceny priorytetu `calculateChunkPriorityScore(dx, dy, dz)` w [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java):
    - Poziom oczu (`dy = 0`) i gruntu pod stopami (`dy = -1`) ma priorytet bezwzględny.
    - Chunki podziemne (`dy < -2`) oraz chmury/niebo otrzymują wysokie kary priorytetu (`32 + (-dy) * 40`), dzięki czemu są generowane asynchronicznie w tle w ramach 3.5ms budżetu klatki.
  - Zastąpiono potrójne zagnieżdżone pętle iterujące od `dy = -renderDistanceY` prekompilowaną, posortowaną tablicą `sortedChunkOffsets` (przeliczaną tylko przy zmianie dystansu widzenia).
  - Przebudowano sortowanie siatek (`dirtyChunks.sort`) na priorytet pionowo-odległościowy.
  - Zaktualizowano `generateInitialChunks()` oraz teleporter gracza (`/tp`) w [`ChatSystem.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/ChatSystem.java), aby generować strefę 7x7 wokół celu priorytetowo przed lądowaniem gracza.

---

### 2. Konfigurowalny system zapisu świata (Przełącznik formatu zapisu)
- [x] **Zrealizowano:**
  - Wdrożono dwuformatowy system zapisu zarządzany przez [`WorldSaveManager.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/WorldSaveManager.java) i [`RegionFileManager.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/RegionFileManager.java):
    - **Format A (Domyślny - Różnice bloków `.dat`):** Zapisuje wyłącznie zmienione przez gracza bloki (`modifiedBlocks`) w formacie bity-na-blok z nagłówkiem `VOX4`.
    - **Format B (Format Regionów `.mca` / Region Storage):** Zapisuje całe zmodyfikowane chunki $16 \times 16 \times 16$ skompresowane algorytmem Deflate (zlib) do plików regionów `r.<rx>.<rz>.mca` w katalogu `saves/<świat>_regions/` z nagłówkiem `VOXR`. Pozwala na bezpośrednie wczytywanie chunków z dysku z pominięciem generatora szumu `TerrainGenerator`.
  - Dodano przełącznik formatu w menu opcji [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) w zakładce **System i Zapis** (`< Format zapisu swiata: Roznice (.dat) / Regiony (.mca) >`).
  - Zintegrowano pole w [`GameSettings.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/GameSettings.java) (`saveFormat = 0 / 1`), zapisywane w `config/options.properties`.
  - Wyświetlanie aktualnego formatu zapisu na liście światów w menu głównym [`MainMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/MainMenu.java).
  - Zagwarantowano 100% kompatybilność wsteczną (`VOX1` - `VOX4` oraz `VOXR`).
  - Szczegółowa specyfikacja binarna: [`docs/REGION_STORAGE_FORMAT.md`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/REGION_STORAGE_FORMAT.md).

---

## 🛠️ Zadania Uzupełniające (Roadmapa Silnika)

### 3. Generator Świata i Biomy
- [ ] **Rzeki i zbiorniki wodne śródlądowe:** Dedykowany szum rzek 2D (`RIVER_FREQ`) z gładkimi korytami wodnymi łączącymi się z oceanem.
- [ ] **Płynna fizyka wody:** Rozprzestrzenianie się wody na sąsiednie bloki w dół i na boki (automatyczny update poziomu płynu).
- [ ] **Podwodna roślinność i dekoracje:** Dodanie koralowców w ciepłych oceanach oraz wodorostów / traw morskich.
- [ ] **Trzcina cukrowa (Sugar Cane):** Dodanie bloku trzciny generującej się na piasku i trawie bezpośrednio przy wodzie.

### 4. Wydajność i Potok Renderowania
- [x] **Frustum Culling (JOML SIMD):** Zaimplementowano w `World.renderChunks` (odrzuca 75-95% niewidocznych chunków).
- [x] **Empty Mesh Culling:** Pomijanie pustych chunków powietrza i litej skały (`c.hasAnyMesh()`).
- [x] **Wielowątkowe generowanie terenu i siatek (Multithreaded Generation & Meshing):**
  - Przeniesiono generację terenu oraz budowę siatek (`BuiltChunkMesh`) do puli wątków roboczych (`ExecutorService workerPool`, 2-8 wątków CPU).
  - Wątki robocze operują na CPU vertex bufferach (`FloatList`) bez odwołań do kontekstu OpenGL, co eliminuje wyścigi i awarie sterownika GPU.
  - Główny wątek renderowania zajmuje się wyłącznie szybkim uploadem gotowych buforów na GPU z kolejki `readyMeshes` w ramach budżetu klatki (2.5 ms, do 64 siatek na klatkę).
  - Wyeliminowano narzut Garbage Collectora: `TerrainGenerator.GeneratorContext` oraz `ChunkMesher.MesherContext` w `ThreadLocal` z płaskimi tablicami 1D i operacjami bitowymi (zero alokacji obiektów na stercie per chunk!).
  - Zoptymalizowano `SimplexNoise` funkcją `fastFloor` oraz wdrożono szybką ścieżkę dla kolumn niezmodyfikowanych w `findTopOpaqueYAt`.
  - Przepustowość wielowątkowa: **ponad 2,000 – 5,600 chunków na sekundę** (generacja 512 chunków w ~250 ms)!
- [x] **In-game Benchmark & Live Profiler:**
  - Dodano komendę `/bench` (oraz `/benchmark [chunki]`, `/bench live`) uruchamiającą wielowątkowy test wydajności silnika na wątkach CPU bez zacinania gry.
  - Wyświetlanie szczegółowego raportu w oknie czatu (czas generacji terenu, tempo w chunk/s, czas budowy siatek, łączna przepustowość i ocena A+/A/B/C).
  - Wbudowano metryki benchmarku bezpośrednio do panelu Debug HUD (`F3`) pod specyfikacją karty graficznej/GPU na prawej stronie (`BENCHMARK`).
  - Rozszerzono panel debugowania `F3` o metryki na żywo: czasy `Gen/Mesh/GPU`, stan kolejek asynchronicznych oraz potencjalną przepustowość w `ch/s`.
- [ ] **Occlusion Culling (Chunki zasłonięte terenem):** Test widoczności dla chunków całkowicie zasłoniętych wzgórzami.

### 5. UI, Debug HUD, Menu Kreatywne i Lokalizacja
- [x] **Debug HUD - Modułowa konfiguracja w Opcjach:**
  - Wdrożono model [`DebugHudConfig`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/DebugHudConfig.java) z 15 osobnymi elementami (`FPS`, `POSITION`, `CHUNK`, `FACING`, `CHUNKS_3D`, `BIOME`, `LIGHT`, `MOVEMENT`, `TIME`, `TARGET`, `SEED`, `MEMORY`, `HARDWARE`, `GPU`, `BENCHMARK`).
  - Dodano dedykowaną zakładkę `Debug HUD` w menu opcji [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java):
    - Włączanie/wyłączanie poszczególnych elementów: `[X] / [ ]`
    - Zmiana strony wyświetlania: `< Lewa > / < Prawa >`
    - Zmiana kolejności: przyciski w górę `[^]` i w dół `[v]`
    - Przycisk przywracania domyślnego układu.
  - Benchmark silnika umieszczony domyślnie na prawej stronie bezpośrednio pod specyfikacją sprzętową i kartą graficzną GPU.
- [x] **Powiększone Menu Kreatywne (9 slotów w rzędzie + Hotbar):**
  - Przebudowano [`InventoryMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/InventoryMenu.java) na układ 9 kolumn (`COLS = 9`, szerokość okna 492px).
  - Hotbar podglądu na dole okna (9 slotów po 44px z marginesami 6px) mieści się idealnie wewnątrz okna kreatywnego.
  - Usunięto nieczytelne numerki ID renderowane na teksturach bloków w siatce wyboru.
- [x] **Autouzupełnianie i sugestie komend w czacie (Autocompletion):**
  - Rozbudowano [`ChatSystem.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/ChatSystem.java) o system podpowiedzi `Suggestion` i obsługę klawisza `Tab`.
  - Podczas wpisywania `/` wyświetla się okienko podpowiedzi nad polem tekstowym z dopasowanymi komendami, ich opisem i parametrami.
  - Obsługa uzupełniania parametrów komend (np. `/time day/noon/...`, `/locate ocean/...`, `/bench 16/...`, `/setblock stone/...`).
  - Nawigacja po liście podpowiedzi strzałkami `Góra / Dół` oraz automatyczne uzupełnianie klawiszem `Tab`.
- [x] **Zapis stanu gracza i reguł gry (Gamerules) z opcjami Override:**
  - W [`WorldSaveManager.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/WorldSaveManager.java) zapisywane są dane gracza (pozycja, rotacja, latanie, noclip, hotbar) oraz reguły świata (`timeOfDay`, `dayCyclePaused`) z nagłówkiem `VOX5` i `VOXR`.
  - W menu opcji dodano zakładkę `Reguły i Overrides`:
    - Główny przycisk: **Wymuszenia reguł (Disable All Overrides)** wyłączający wszystkie wymuszenia jednym kliknięciem.
    - Opcje wymuszenia czasu świata (`overrideTime`), zatrzymania/wznowienia cyklu dobowego (`overrideDayCycle`), wymuszenia latania (`overrideFly`) oraz trybu przenikania przez ściany (`overrideNoclip`).
    - Gdy wymuszenia są wyłączone, stan gry w 100% zależy od reguł zapisanych w pliku świata.
- [x] **Lokalizacja językowa JSON (I18n) i domyślny język angielski:**
  - Utworzono moduł [`I18n.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/i18n/I18n.java) z własnym lekki parserem JSON bez zewnętrznych bibliotek.
  - Pliki tłumaczeń: `assets/lang/en_us.json` (domyślny angielski) oraz `assets/lang/pl_pl.json` (polski) - dostępne zarówno z zasobów JAR, jak i do bezpośredniej edycji w katalogu roboczym `assets/lang/`.
  - Przełącznik języka w menu opcji w zakładce `System i Język` z natychmiastowym przeładowaniem wszystkich interfejsów (Menu Główne, Pauza, Opcje, Ekwipunek, HUD, Czat).
- [x] **Automatyczne dopasowywanie tekstu w przyciskach (Auto-fit Button Text):**
  - W [`TextRenderer.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/TextRenderer.java) metoda `drawTextCenteredInBox(...)` rygorystycznie pilnuje marginesów wewnętrznych (`hMargin >= 6.0px`), uwzględnia cień tekstu (`+scale`) oraz automatycznie skaluje tekst w dół z obcięciem wielokropkiem `...` jako ostatecznym zabezpieczeniem, uniemożliwiając wyjście tekstu poza ramki przycisków i zakładek.
  - Na ekranie wyboru świata w [`MainMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/MainMenu.java) dolne przyciski akcji otrzymały proporcjonalne, dynamiczne szerokości dostosowane do długości etykiet (np. "Play Selected World" / "Graj na wybranym swiecie").
  - Zakładki w [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) otrzymały 4px odstępy separujące i rygorystyczne centrowanie wewnątrz własnych pól, eliminując nakładanie się sąsiadujących kategorii.
- [x] **Interaktywne poziome suwaki (Sliders) w menu opcji (Task 21):**
  - W [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) zastąpiono przyciski cykliczne `< Value >` ciągłymi suwakami poziomymi z przeciąganym uchwytem dla opcji ciągłych:
    - *Render Distance* (2 do 16 chunków)
    - *FOV* (60° do 110° Quake Pro)
    - *Brightness* (50% do 150%)
    - *Mouse Sensitivity* (0.04 do 0.40)
    - *Mouse Hold Delay* (50ms do 450ms)
  - Obsłużono klikanie w dowolny punkt paska suwaka oraz płynne przeciąganie myszą (`handleMouseMove` i `handleMouseRelease`).
- [x] **Dynamiczne przeliczanie układu GUI przy zmianie rozmiaru okna (Task 22):**
  - W [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java) w wywołaniu zwrotnym `glfwSetFramebufferSizeCallback` dodano metodę `recalculateGuiLayout()`.
  - Wprowadzono metodę `onResize(int vWidth, int vHeight)` we wszystkich interfejsach (`MainMenu`, `OptionsMenu`, `PauseMenu`, `InventoryMenu`, `ChatSystem`, `NoiseVisualizer`), gwarantującą natychmiastową adaptację okien, suwaków i stref klikalnych do nowej rozdzielczości lub skali GUI.
- [x] **Płynne przewijanie list (Scroll zamiast stron poza głównym menu opcji):**
  - W konfiguratorze Debug HUD w [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) usunięto przyciski stron (`< Page 1/2`, `Page 2 >`) na rzecz płynnej listy przewijanej rolką myszy z paskiem przewijania (scrollbar).
  - Na ekranie wyboru światów w [`MainMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/MainMenu.java) zastąpiono sztywny limit 6 zapisów pełnym przewijaniem z suwakiem.
  - W ekwipunku kreatywnym [`InventoryMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/InventoryMenu.java) dodano obsługę scrollowania rzędów.
- [x] **Skalowanie interfejsu (GUI Scale):**
  - W [`GameSettings.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/GameSettings.java) dodano `guiScale` (`0 = Auto`, `1x`, `2x`, `3x`, `4x`) oraz wyliczanie skali efektywnej `getEffectiveGuiScale(...)`.
  - W opcjach graficznych [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) dodano przycisk wyboru skali GUI.
  - W [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java) rzutowanie ortograficzne oraz współrzędne myszy automatycznie adaptują się do skali GUI w 100% natywnej ostrości.

### 6. Rozgrywka i Mechanika
- [x] **Naprawa "Save and Quit to Title" zamykającego grę:**
  - Naprawiono propagację zdarzeń zwolnienia przycisku myszy (`GLFW_RELEASE`) w pętli obsługi okna w [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java), zabezpieczając kliknięcia warunkiem `action == GLFW_PRESS`.
  - Wprowadzono 250ms bufor czasowy (`openCooldownUntil`) w [`MainMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/MainMenu.java), [`PauseMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/PauseMenu.java) i [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) eliminujący przenikanie kliknięć (click bleed-through) między nakładającymi się przyciskami.
  - Naprawiono obsługę nagłówka `MAGIC_VOX5` w [`WorldSaveManager.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/WorldSaveManager.java).
- [x] **Generator Oceanu (Wykładniczy profil głębokości):**
  - W [`TerrainGenerator.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/gen/TerrainGenerator.java) zastąpiono liniowy spadek dna oceanu wykładniczą krzywą szelfu kontynentalnego (`smoothstep(0.10, 0.55, u)`).
  - W strefie przybrzeżnej głębokość wody wynosi 1–2 bloki (płytkie brodzenie), a w odległości kilkunastu bloków od brzegu dno gwałtownie opada w otchłań oceanu o głębokości 24–28 bloków pod poziomem morza.
- [x] **Góry i Biomy Śnieżne (Wysokie szczyty i tajgowe świerki):**
  - W [`TerrainGenerator.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/gen/TerrainGenerator.java) zwiększono skale wzniesień górskich (`mountElevation`) do 72.0.
  - Granica śniegu podniesiona z Y=55 na **Y=94**, a granica litej skały na **Y=84**, dzięki czemu szczyty górskie są monumentalne i strzeliste.
  - Na zboczach górskich wprowadzono subalpejskie trawiaste polany tajgowe (`BlockType.TAIGA_GRASS`) z gęstym drzewostanem świerkowym (`placeSpruceTreeSlice`), a na wysokich szczytach śnieżnych rzadkie, wytrzymałe świerki.
- [x] **Pauza czasu świata:**
  - W [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java) podczas pauzy gry (`pauseMenu.isOpen()` lub menu opcji) do metody `world.update(pos, dt)` przekazywany jest delta-time `0.0f`.
  - Upływ czasu `timeOfDay` zostaje całkowicie zamrożony w stanie pauzy.
- [x] **Więcej rodzajów kwiatów:**
  - Dodano 4 nowe gatunki kwiatów w [`BlockType.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/BlockType.java): Czerwona róża (`ROSE`, id 31), Mniszek lekarski (`DANDELION`, id 32), Niebieska orchidea (`BLUE_ORCHID`, id 33), Czosnek ozdobny (`ALLIUM`, id 34).
  - Wszystkie posiadają model skrzyżowany (`crossModel = true`) oraz dedykowane tekstury w [`TextureAtlas.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/TextureAtlas.java) (kafelki 38–41).
  - Dodano pełne tłumaczenia w `en_us.json` i `pl_pl.json` oraz rozproszenie w generatorze biomów.
- [x] **Fizyka płynów i bloków (/worldsetting):**
  - W [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java) zaimplementowano asynchroniczną kolejkę fizyki `physicsQueue` i metodę `tickPhysics(128)`.
  - Piasek (`SAND`, `RED_SAND`) grawitacyjnie spada przez powietrze oraz tonie w wodzie.
  - Woda obsługuje dwa tryby konfigurowane przez komendę `/worldsetting water <minecraft|terraria>`:
    - `minecraft`: klasyczne rozlewanie się wody na sąsiednie bloki.
    - `terraria`: zachowanie stałej objętości cieczy (przemieszczanie się słupa wody w dół i na boki do niecki).
  - Przełącznik fizyki `/worldsetting physics <on|off>`.
- [x] **Animowana tekstura wody (Shader + Dynamic UV Ripple):**
  - W [`ShaderProgram.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/ShaderProgram.java) dodano uniform `uniform float uTime` oraz dwufalowe zakłócenia falowe UV i słoneczny połysk specular w shaderze fragmentów dla kafelka wody.
- [x] **Ciągłe niszczenie/stawianie bloków (Mouse Hold):**
  - W [`GameSettings.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/GameSettings.java) dodano `mouseHoldDelay` (domyślnie 0.22s / 220ms - standard Minecrafta).
  - Możliwość konfiguracji w zakładce Sterowanie w [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) (150ms – 350ms).
  - Ciągłe wywoływanie interakcji przy wciśniętym LPM/PPM w pętli gry [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java).
- [x] **Ekran wyboru świata w stylu Minecraft:**
  - Usunięto podwójny komunikat o wczytaniu świata na czacie (usunięto nadmiarowe ładowanie wstępne w `Main.java` oraz dodano `chatSystem.clear()`).
  - Menu główne [`MainMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/MainMenu.java) wyświetla teraz przycisk "Select World" / "Wybierz swiat", prowadzący do pełnej listy światów z obsługą grania (w tym podwójne kliknięcie), tworzenia nowych światów oraz bezpiecznego usuwania.
- [ ] **Dźwięki bloków i otoczenia:** Dźwięki stawiania/niszczenia bloków (trawa, kamień, drewno, piasek, woda) oraz dźwięki kroków i pluskania.
- [ ] **Ekwipunek i Crafting:** Rozbudowa ekranu ekwipunku (`E`) o siatkę tworzenia przedmiotów (crafting 2x2 i stół rzemieślniczy 3x3).
- [ ] **Płynne oświetlenie (Smooth Lighting):** Rozszerzenie cieniowania Ambient Occlusion o propagację światła słonecznego i blokowego (światło zalewowe BFS / Flood-fill).

---

## 🚀 Zrefinowane Zadania Deweloperskie (Z notatnika `developer todo.md` #21–#27)

Sekcja ta zawiera precyzyjne specyfikacje techniczne dla agentów implementujących otwarte pomysły z notatnika dewelopera.

### #21. Slidery zamiast przycisków cyklicznych w menu opcji UI
- [x] **Zrealizowano:**
  - W [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) wprowadzono interaktywne suwaki (`drawSlider`) dla dystansu renderowania (2–16 chunków), pola widzenia (FOV 60°–110°), jasności (50%–150%), czułości myszy oraz opóźnienia niszczenia/stawiania bloków (50ms–450ms) z obsługą przeciągania myszą.

---

### #22. Dynamiczny przelicznik layoutu GUI przy zmianie rozmiaru okna (`onResize`)
- [x] **Zrealizowano:**
  - W [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java) podpięto wywołania `onResize(vWidth, vHeight)` w procedurze adaptacji rzutowania ortograficznego do skali GUI i wymiarów okna.
  - Okna menu, ekwipunku i czatu dynamicznie wyliczają centrowanie i wymiary proporcjonalnie do rozdzielczości ekranu.

---

### #23. Assety jako pliki PNG z dysku (`run/assets/textures/block/`) + migracja na `GL_TEXTURE_2D_ARRAY`
- [ ] **Specyfikacja zadania:**
  - **Cel:** Zastąpienie proceduralnego generowania kafelków w kodzie Java ([`TextureAtlas.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/TextureAtlas.java)) ładowaniem plików graficznych PNG z dysku oraz przejściem na tablicę tekstur OpenGL (`GL_TEXTURE_2D_ARRAY`).
  - **Kroki implementacji:**
    1. Dodanie zależności `org.lwjgl:lwjgl-stb` do `pom.xml`.
    2. Utworzenie katalogu zasobów `run/assets/textures/block/` (oraz fallback w `src/main/resources/assets/textures/block/`).
    3. Wyeksportowanie/utworzenie bazowych tekstur w formacie PNG $16 \times 16$ (kamień, ziemia, trawa, drewno, liście, woda, kwiaty, ruda, etc.).
    4. Zaimplementowanie `TextureArrayLoader`:
       - Tworzy teksturę `GL_TEXTURE_2D_ARRAY` o rozmiarach $16 \times 16 \times N$ (gdzie $N$ to liczba warstw/bloków).
       - Każda tekstura to dedykowana warstwa (layer) indeksowana liczbą całkowitą.
       - Generuje pełne mipmapy (`glGenerateMipmap(GL_TEXTURE_2D_ARRAY)`). Zero texture bleedingu i szwów!
    5. Aktualizacja shadera bloków ([`ShaderProgram.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/ShaderProgram.java)):
       - Sampler w shaderze fragmentów: `uniform sampler2DArray uTextureArray`.
       - Próbkowanie: `texture(uTextureArray, vec3(inTexCoord.xy, inLayerIndex))`.
    6. Zabezpieczenie: Brakujący plik ładuje domyślną teksturę szachownicy magenta/czarny (`missing.png`).
  - **Kryteria ukończenia (DoD):**
    - Wszystkie tekstury czytane są z plików PNG. Dodanie nowego pliku PNG do katalogu umożliwia natychmiastowe wykorzystanie go w grze bez ingerencji w kod proceduralny atlasu.

---

### #24. Globalna modułowa architektura rejestrów i obiektów gry (Bloki, Itemy, Encje / Moby)
- [ ] **Specyfikacja zadania:**
  - **Cel:** Odejście od monolitycznych klas i sztywnych enumów na rzecz uogólnionego, modularnego systemu rejestrów i hierarchii obiektów dla **całego projektu** (przygotowując silnik na bloki, przedmioty oraz encje/moby z AI, pathfindingiem i renderingiem).
  - **1. Generyczny system rejestrów (`Registry<T>` / `Registries`):**
    - Wprowadzenie centralnego systemu rejestracji opartego o unikalne klucze przestrzeni nazw `Identifier` (np. `"voxel:zombie"`, `"voxel:dirt"`, `"voxel:iron_sword"`).
    - Rejestry wbudowane w `com.voxel.registry.Registries`:
      - `Registries.BLOCK`: Rejestr instancji `Block`.
      - `Registries.ITEM`: Rejestr instancji `Item`.
      - `Registries.ENTITY_TYPE`: Rejestr typów encji `EntityType<?>`.
    - Dwukierunkowe mapowanie: `Identifier <-> Instancja Obiektu <-> Numeryczny ID / State ID` dla wydajności zapisu i pętli gry.
  - **2. Modułowa architektura bloków i stanów:**
    - Klasa bazowa `public abstract class Block` (`isSolid()`, `isTransparent()`, `getCollisionAABB()`, `onNeighbourChanged()`, etc.).
    - Klasy wyspecjalizowane: `FullBlock`, `PlantBlock`, `LeavesBlock`, `FluidBlock`.
    - Klasa `BlockState`: niemutowalna kombinacja `(Block, StateProperties)` z prealokowanym `stateId` (0..65535).
  - **3. Architektura Encji i Mobów (`com.voxel.entity`):**
    - Klasa bazowa `public abstract class Entity`:
      - Fizyka i transformata: pozycja `(x, y, z)`, prędkość `(vx, vy, vz)`, rotacja `(yaw, pitch)`, `AABB` (kolizje z blokami wokselowymi).
      - Flagi stanu: `onGround`, `inWater`, `isRemoved()`.
      - Cykl życia: `tick(World world, float dt)`, `onRemove()`.
    - Klasa pośrednia `public abstract class LivingEntity extends Entity`:
      - Parametry życiowe: `health`, `maxHealth`, `isDead()`, `takeDamage(float amount, DamageSource src)`.
      - Animacje: czerwone mignięcie po trafieniu (`hurtTime`), interpolacja obrotu głowy i ciała.
    - Klasa bazowa dla mobów `public abstract class MobEntity extends LivingEntity`:
      - AI & Zachowania: pętla decyzyjna `updateAi()`, cele (`GoalSelector` / `AiGoal` np. `WanderGoal`, `MeleeAttackGoal`, `LookAtPlayerGoal`).
      - Pathfinding: prosty nawigator w przestrzeni 3D wokseli (`MobNavigator`).
    - Fabryka typów encji `public final class EntityType<T extends Entity>`:
      - Rejestrowana w `Registries.ENTITY_TYPE`.
      - Metoda `create(World world) -> T`.
    - Integracja w `World.java`:
      - Zarządzanie encjami: `List<Entity> entities`, dodawanie/usuwanie i symulacja w `worldTick(dt)`.
      - Renderowanie encji: podział na dedykowane renderery `EntityRenderer<T>` w fazie renderowania świata.
  - **4. Architektura Przedmiotów i Ekwipunku (`com.voxel.item`):**
    - Klasa bazowa `Item` (maksymalny stos `maxCount`, zachowanie przy użyciu `use()`).
    - `BlockItem` (przedmiot reprezentujący blok w ręku).
    - `ItemStack(Item item, int count)`.
  - **Kompatybilność wsteczna formatów zapisu:**
    - Zachowanie mapowania starych ID (1..42) z nagłówków `VOX1`–`VOX4` i `VOXR` na stany `BlockState`.
  - **Kryteria ukończenia (DoD):**
    - Projekt posiada ujednolicony system rejestrów.
    - Dodanie nowego bloku, przedmiotu lub moba (np. `ZombieEntity` czy `PigEntity`) polega na utworzeniu dedykowanej podklasy i zarejestrowaniu jej w `Registries`, bez modyfikowania istniejących klas i switch-statementów w `Main.java` czy `World.java`.

---

### #25. Dynamiczne uśredniane tintowanie koloru trawy (Biome Colormap Tinting)
- [ ] **Specyfikacja zadania:**
  - **Cel:** Likwidacja sztucznych, osobnych bloków `TAIGA_GRASS` na rzecz jednego bazowego bloku `GRASS`, którego wierzchołki są dynamicznie barwione w zależności od temperatury i wilgotności biomu z płynnymi przejściami.
  - **Implementacja:**
    1. Utworzenie mapy kolorów (colormap) lub matematycznego gradientu:
       - Ciepłe równiny: soczysta zieleń (`#79C05A`).
       - Chłodna tajga: stonowana, ciemniejsza zieleń iglasta (`#597D45`).
       - Pustynia / suche biomy: żółtawo-oliwkowa zieleń.
    2. Uśrednianie biomów (Smooth Transition):
       - W [`ChunkMesher.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/mesh/ChunkMesher.java) podczas budowy górnej ściany trawy, próbkowana jest temperatura i wilgotność w promieniu 2 bloków wokół kolumny (filtr $5 \times 5$).
       - Wyliczony kolor RGB jest przekazywany do bufora wierzchołków (`FloatList`) jako `inColorTint`.
    3. Shader fragmentów mnoży bazową teksturę trawy przez kolor wierzchołka: `fragColor = texColor * vec4(inColorTint, 1.0)`.
  - **Kryteria ukończenia (DoD):**
    - Brak ostrych granic kolorystycznych między biomami na powierzchni lądu; przejścia biomów są w 100% płynne i organiczne.

---

### #26. Zaśnieżony blok trawy (`snowy grass block`)
- [ ] **Specyfikacja zadania:**
  - **Cel:** Wprowadzenie zaśnieżonego wariantu bloku trawy, który pojawia się naturalnie pod pokrywą śnieżną w zimnych biomach i wysokich partiach gór.
  - **Implementacja:**
    - Wykorzystanie stanu `BlockState` trawy z właściwością boolean `snowy`:
      - `snowy = false`: klasyczna zielona tekstura górna + trawiasty bok.
      - `snowy = true`: biała tekstura śniegu na górze + specjalna boczna tekstura trawy z ośnieżoną górną warstwą.
    - W generatorze terenu ([`TerrainGenerator.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/gen/TerrainGenerator.java)): Gdy na wysokości `Y >= 94` lub w biomie tundry na trawie generuje się warstwa `BlockType.SNOW`, blok pod spodem przyjmuje stan `snowy = true`.
    - W systemie aktualizacji bloków: Usunięcie śniegu nad blokiem trawy przywraca stan `snowy = false`.
  - **Kryteria ukończenia (DoD):**
    - Trawa pod śniegiem posiada autentyczny, zaśnieżony wygląd ścianek bocznych identycznie jak w grze Minecraft.

---

### #27. Zoptymalizowany system aktualizacji bloków (Neighbour Updates & Scheduled Ticks)
- [ ] **Specyfikacja zadania:**
  - **Cel:** Zastąpienie sztywnej metody `tickPhysics` w [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java) uogólnionym, wysoce zoptymalizowanym systemem dwutorowych aktualizacji świata.
  - **Struktura systemu:**
    1. **Powiadomienia sąsiadów (`Neighbour Updates`):**
       - Metoda `world.notifyNeighbours(x, y, z)` wywołująca `onNeighbourChanged()` dla 6 przyległych bloków (góra, dół, północ, południe, wschód, zachód).
       - Rośliny i kwiaty ([`PlantBlock`]) natychmiast weryfikują stabilność podłoża (`y - 1`). Jeśli podłoże zostało zniszczone, roślina odpada i zamienia się w powietrze.
    2. **Planowane ticki (`Scheduled Ticks`):**
       - Kolejka priorytetowa `PriorityQueue<ScheduledTick>` w `World.java`.
       - Każde zdarzenie posiada: `(targetWorldTick, blockPos, blockType, priority)`.
       - Zastosowanie: opadanie piasku/żwiru z opóźnieniem (np. 2 ticki), rozlewanie płynów, topnienie lodu pod wpływem ciepła.
    3. **Ochrona przed kaskadowym spadkiem FPS (Tick Budgeting):**
       - Sztywny limit maksymalnej liczby wykonanych aktualizacji na tick świata (np. `MAX_UPDATES_PER_TICK = 256`).
       - Wszystkie zdarzenia powyżej limitu pozostają w kolejce i są przetwarzane w kolejnych tickach, co gwarantuje stałe 20 TPS nawet przy zawaleniu wielkich jaskiń piaskowych.
  - **Kryteria ukończenia (DoD):**
    - Zniszczenie bloku pod kwiatem natychmiastowo niszczy kwiat.
    - Kaskady aktualizacji nie zamrażają pętli gry.

---

### #28. Naprawa formatowania wartości sliderów (`%.0f` format specifier mismatch w `I18n` i `OptionsMenu`)
- [ ] **Specyfikacja zadania:**
  - **Problem:** W plikach tłumaczeń `en_us.json` oraz `pl_pl.json` klucze suwaków `options.fov` (`"FOV: %.0f deg"`) oraz `options.brightness` (`"Brightness: %.0f%%"`) używają specyfikatora `%f` (dla liczb zmiennoprzecinkowych). W klasie [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) do metody `I18n.t(...)` przekazywane są natomiast liczby całkowite `int` (`fovVal`) lub `String` (`brightPercent + "%"`).
  - W efekcie metoda `String.format(Locale.ROOT, val, args)` w [`I18n.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/i18n/I18n.java) rzuca wyjątek `IllegalFormatConversionException` (przechwytywany cicho w bloku `catch`), przez co suwaki wyświetlają surowy wzorzec `FOV: %.0f deg` zamiast sformatowanej wartości liczbowej.
  - **Plan naprawy:**
    1. Standaryzacja typów w plikach językowych `en_us.json` i `pl_pl.json`:
       - Zmiana `%f` / `%.0f` na `%s` lub `%d` (np. `"options.fov": "FOV: %s"`, `"options.brightness": "Brightness: %s"`).
    2. Spójne formatowanie w `OptionsMenu.java`:
       - Formatowanie wartości numerycznych przed przekazaniem do `I18n.t` (np. `String.format("%d deg", fovVal)` lub bezpośrednio jako `int`).
    3. Przegląd wszystkich pozostałych kluczy używających `%.0f` (benchmarki w `ChatSystem`, `HudRenderer`, etc.) i zapewnienie zgodności typów argumentów.
  - **Kryteria ukończenia (DoD):**
    - Suwaki FOV, Jasności, Czułości i Dystansu renderowania w menu Opcji wyświetlają poprawne, czytelne wartości liczbowe bez surowych znaczników `%.0f`.

---

### #29. Kompletny i niezawodny zapis wszystkich ustawień gry do pliku (`options.properties`)
- [ ] **Specyfikacja zadania:**
  - **Problem:** Część ustawień gry nie jest trwale zapisywana do pliku konfiguracyjnego `config/options.properties`:
    - W `GameSettings.save()` brakuje zapisu klawisza pauzy (`keyPause = GLFW_KEY_ESCAPE`) oraz synchronizacji niektórych opcji zmienianych w trakcie gry.
    - Zmiana ustawień przez suwaki i przyciski nie zapisuje się automatycznie przy nagłym zamknięciu okna gry (krzyżykiem `X` na pasku okna GLFW), ponieważ w pętli `Main.java` przy wyjściu zapisywany jest tylko stan świata (`world.saveWorld`), a nie ustawienia gry (`GameSettings.save()`).
  - **Plan naprawy:**
    1. Dodanie brakujących pól w `GameSettings.java`:
       - Zapis i odczyt `keyPause` w `GameSettings.save()` oraz `GameSettings.load()`.
       - Zapewnienie, że wszystkie nowe i istniejące opcje (w tym audio, gamerule overrides, debug HUD layout) są w 100% zserializowane.
    2. Zapis w pętli życia gry:
       - Dodanie wywołania `GameSettings.save()` w procedurze zamykania okna / pętli głównej w [`Main.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/Main.java) (zarówno w `cleanup()` jak i po zakończeniu pętli `while (!glfwWindowShouldClose(window))`).
       - Opcjonalne wywołanie `GameSettings.save()` w `OptionsMenu.handleMouseRelease()` przy puszczeniu suwaka, aby zapobiec utracie zmian suwaków.
  - **Kryteria ukończenia (DoD):**
    - Dowolna modyfikacja ustawień (grafika, sterowanie, klawisze, fizyka wody, reguły gry) przetrwa restart gry, niezależnie od tego, czy gracz zamknął menu przyciskiem "Done", klawiszem ESC, czy zamknął okno krzyżykiem systemowym `X`.
