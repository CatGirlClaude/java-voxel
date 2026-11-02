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
- [x] **Rzeki i zbiorniki wodne śródlądowe:**
  - Wdrożono dedykowany dwuoktawowy szum rzek 2D (`RIVER_FREQ = 0.0022`) oraz biom [`Biome.RIVER`].
  - Płynne żłobienie koryt rzecznych w terenie do głębokości 3–4 bloków poniżej poziomu morza (`SEA_LEVEL = 28`), naturalnie wypełniających się wodą i łączących się bezpośrednio z oceanem.
  - Naturalne dno rzek zbudowane ze żwiru (`GRAVEL`), gliny (`CLAY`), piasku i ziemi.
- [x] **Płynna fizyka wody:**
  - Zaimplementowano trójtrybową fizykę wody w [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java): modern Minecraft (stany przepływu 1..7, blok źródłowy 8, cofanie przy braku zasilania), preclassic/classic (nieskończone zalewanie) oraz Terraria (zachowanie objętości cieczy).
  - Płynne wymywanie roślin i dekoracji.
- [x] **Podwodna roślinność i dekoracje:**
  - Dodano koralowce ([`CORAL`]) generujące się w ciepłych strefach oceanicznych.
  - Dodano trawę morską ([`SEAGRASS`]) oraz wysokie kolumny wodorostów ([`KELP`]) rosnące w oceanach i rzekach.
  - Pełna obsługa waterloggingu – gracz pływający wodorostami i trawą morską płynnie przebywa w wodzie bez przerw w tafli.
- [x] **Trzcina cukrowa (Sugar Cane):**
  - Wprowadzono blok [`SugarCaneBlock`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/SugarCaneBlock.java) (ID: 42) generujący się na piasku, ziemi i trawie bezpośrednio przy wodzie wzdłuż brzegów rzek i oceanów na wysokość 2–3 bloków.
  - Sprawdzanie warunków przetrwania i natychmiastowe niszczenie przy utracie podłoża lub braku wody.

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
- [x] **Occlusion Culling (Chunki zasłonięte terenem):**
  - Wdrożono dwustopniowy, zerokosztowy test przesłaniania w [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java) (`isChunkOccluded`):
    - Subterranean Culling: natychmiastowe odrzucanie głębokich chunków podziemnych i jaskiniowych, gdy gracz porusza się po powierzchni ziemi i nie patrzy w pionowy szyb.
    - Mountain Ridge Culling: odrzucanie odległych chunków dolin zasłoniętych wysokimi grzbietami górskimi.
    - Zero narzutu CPU/GPU: operacje czysto arytmetyczne korzystające z prekompilowanego bufora `findTopOpaqueYAt`.

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
- [x] **Płynne oświetlenie (Smooth Lighting):**
  - W [`ChunkMesher.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/mesh/ChunkMesher.java) zaimplementowano propagację światła słonecznego oraz blokowego algorytmem BFS (`propagateLight`).
  - Wierzchołkowe wygładzanie oświetlenia (`packVertexLight`) uśredniające 4 sąsiednie próbki światła wokół każdego narożnika ścianki wraz z testami przesłaniania krawędzi (Ambient Occlusion + Smooth Lighting).

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

### #23. Assety jako pliki PNG z dysku (`run/assets/textures/block/`)
- [x] **Zrealizowano:**
  - W [`TextureAtlas.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/TextureAtlas.java) wdrożono ładowanie tekstur bloków bezpośrednio z plików PNG (`run/assets/textures/block/` oraz classpath fallback `src/main/resources/assets/textures/block/`).
  - Wyeksportowano wszystkie 42 kafelki bloków jako pliki PNG $16 \times 16$.
  - W przypadku braku pliku automatycznie generowany jest fallback lub szachownica błędu.

---

### #24. Globalna modułowa architektura rejestrów i obiektów gry (Bloki, Itemy, Encje / Moby)
- [x] **Zrealizowano:**
  - Wprowadzono pakiet `com.voxel.registry` z klasami [`Identifier.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/registry/Identifier.java), [`Registry.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/registry/Registry.java), [`SimpleRegistry.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/registry/SimpleRegistry.java) oraz centralnym [`Registries.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/registry/Registries.java) (`BLOCK`, `ITEM`, `ENTITY_TYPE`).
  - Wprowadzono hierarchię bloków w `com.voxel.block`: [`Block.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/Block.java), [`BlockState.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/BlockState.java), [`Blocks.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/Blocks.java) oraz podklasy wyspecjalizowane (`SolidBlock`, `GrassBlock`, `SnowyGrassBlock`, `PlantBlock`, `FallingBlock`, `FluidBlock`, etc.).
  - Wprowadzono hierarchię przedmiotów w `com.voxel.item`: [`Item.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/item/Item.java), [`BlockItem.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/item/BlockItem.java), [`ItemStack.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/item/ItemStack.java), [`Items.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/item/Items.java).
  - Wprowadzono hierarchię encji w `com.voxel.entity`: [`Entity.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/entity/Entity.java), [`LivingEntity.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/entity/LivingEntity.java), [`MobEntity.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/entity/MobEntity.java), [`EntityType.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/entity/EntityType.java), [`EntityTypes.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/entity/EntityTypes.java), zmigrowano `Player` do dziedziczenia po `LivingEntity`.
  - Pełna kompatybilność wsteczna z formatami zapisu (`VOX1`–`VOX5`, `VOXR`) i mostek `BlockType <-> Block`.

---

### #25. Dynamiczne uśredniane tintowanie koloru trawy (Biome Colormap Tinting)
- [x] **Zrealizowano:**
  - Dodano dynamiczne barwienie wierzchołków trawy i wysokiej trawy w oparciu o temperaturę i wilgotność biomów ([`Biome.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/Biome.java)).
  - Rozszerzono format wierzchołków w mesherze i buforach VBO do 10 floatów (`x, y, z, u, v, skyLight, blockLight, r, g, b`).
  - W [`ChunkMesher.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/mesh/ChunkMesher.java) zaimplementowano filtr uśredniający $3 \times 3$ dla płynnych, organicznych przejść kolorystycznych między biomami.
  - Zaktualizowano shader [`ShaderProgram.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/render/ShaderProgram.java) o `layout(location = 4) in vec3 aColor;` i multiplikatywne barwienie pikseli.

---

### #26. Zaśnieżony blok trawy (`snowy grass block`)
- [x] **Zrealizowano:**
  - Wprowadzono blok [`SnowyGrassBlock.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/block/SnowyGrassBlock.java) (tekstura górna: śnieg, spód: ziemia, boki: ośnieżona trawa).
  - Integracja z systemem aktualizacji bloków: położenie śniegu na trawie zamienia ją w zaśnieżoną trawę, zniszczenie śniegu przywraca zwykłą trawę.

---

### #27. Zoptymalizowany system aktualizacji bloków (Neighbour Updates & Physics Checks)
- [x] **Zrealizowano:**
  - W [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java) wprowadzono system `updateNeighbors(wx, wy, wz)` oraz `notifyNeighbor(fromX, fromY, fromZ, toX, toY, toZ)`.
  - Powiązanie z `PlantBlock` / `FlowerBlock`: zniszczenie bloku nośnego pod rośliną powoduje natychmiastowe odpadnięcie i usunięcie rośliny.
  - Powiązanie z `FallingBlock`: utrata podparcia pod blokami opadającymi (piasek, żwir) automatycznie kolejkuje ich opadanie.

---

### #28. Naprawa formatowania wartości sliderów (`%.0f` format specifier mismatch w `I18n` i `OptionsMenu`)
- [x] **Zrealizowano:**
  - Zmieniono znaczniki formatowania `%d` / `%.0f` / `%.2f` na uniwersalne `%s` w `en_us.json` i `pl_pl.json`.
  - W [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) wartości suwaków formatowane są przed przekazaniem do `I18n.t(...)`.
  - Wzmocniono [`I18n.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/i18n/I18n.java) o mechanizm auto-korekty wzorców formatowania w razie niezgodności typów.

---

### #29. Kompletny i niezawodny zapis wszystkich ustawień gry do pliku (`options.properties`)
- [x] **Zrealizowano:**
  - Dodano brakujący klawisz pauzy `keyPause` do serializacji w [`GameSettings.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/config/GameSettings.java).
  - W [`OptionsMenu.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/ui/OptionsMenu.java) dodano natychmiastowy zapis ustawień przy kliknięciach, puszczeniu suwaków oraz resecie domyślnych.
  - Zarejestrowano JVM Shutdown Hook dla `GameSettings.save()`, gwarantujący trwałość konfiguracji nawet przy nagłym zamknięciu okna.
