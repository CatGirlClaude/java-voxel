# Java Voxel 3D — Architektura, Systemy i Roadmapa

Kompleksowy dokument architektoniczny i mapa drogowa silnika voxelowego **Java Voxel 3D**, integrujący i weryfikujący stan faktyczny kodu źródłowego (`src/`), dokumentację techniczną (`docs/TODO.md`, `docs/WORLD_GENERATION.md`, `docs/REGION_STORAGE_FORMAT.md`, `docs/README.md`) oraz zadania deweloperskie (`docs/developer todo.md`).

> [!NOTE]
> **Raport z weryfikacji faktograficznej (Fact-Check analizy Claude'a):**
> Poprzednia wersja robocza została przygotowana bez wglądu w kod źródłowy oraz dokumentację w `docs/`, co doprowadziło do błędnego założenia, że pozycje 1–20 z listy `developer todo.md` są nierozwiązane. W rzeczywistości **zadania 1–20 zostały w 100% zrealizowane, zintegrowane w kodzie i zweryfikowane automatycznymi testami**.
> Niniejszy dokument prostuje nieścisłości, opisuje rzeczywistą architekturę wdrożoną w silniku oraz definiuje realną roadmapę skupioną na pozycjach otwartych (**#21–#27** oraz zadaniach silnikowych z `docs/TODO.md`).

---

## 1. Stan projektu i audyt implementacji

**Stack technologiczny:**
- **Język i środowisko:** Java 21 (zgodność z JDK 21–25), Maven.
- **Grafika i okno:** LWJGL 3.3.4 (GLFW + OpenGL 3.3 Core Profile).
- **Matematyka 3D:** JOML 1.10.5 (wektory, macierze, frustum culling SIMD).
- **Punkt wejścia:** `com.voxel.Main`, skrypt startowy `run.bat`.

### 1.1 Rzeczywisty stan zadań z listy `developer todo.md`

| # | Zadanie deweloperskie | Status w kodzie | Rzeczywista implementacja |
| :--- | :--- | :---: | :--- |
| **1, 10, 11** | Wykładnicza głębokość oceanu, brzeg ≤1 bloku | ✅ **ZREALIZOWANE** | `TerrainGenerator.java`: funkcja `smoothstep(0.10, 0.55, u)`, szelf o głębokości 1–2 bloki, otchłań 24–28 bloków pod poziomem morza (`SEA_LEVEL = 28`). |
| **2** | Tajga w górach, wyższa linia śniegu | ✅ **ZREALIZOWANE** | `TerrainGenerator.java`: góry do Y=85–94, linia śniegu podniesiona na Y=94, świerki (`placeSpruceTreeSlice`) na polanach tajgowych (`TAIGA_GRASS`) i szczytach. |
| **3, 20** | Pauza czasu świata i podział ticków | ✅ **ZREALIZOWANE** | `Main.java`: wyraźny podział na `clientTick(dt)` i `worldTick(isPaused ? 0.0f : dt)`. Czat blokuje ruch gracza, ale świat tyka w tle; menu pauzy zamraża czas świata. |
| **4** | Nowe gatunki kwiatów | ✅ **ZREALIZOWANE** | `BlockType.java`: róża (`ROSE`), mniszek (`DANDELION`), orchidea (`BLUE_ORCHID`), allium (`ALLIUM`) z modelami krzyżowymi (`crossModel = true`) i kafelkami w atlasie (38–41). |
| **5, 13, 15** | Fizyka wody, piasku i niszczenie dekoracji | ✅ **ZREALIZOWANE** | `World.java`: kolejka `physicsQueue`, grawitacja piasku w powietrzu i wodzie, niszczenie roślin przez wodę, komenda `/worldsetting water <modern\|terraria>` oraz `/worldsetting physics <on\|off>`. |
| **6, 14** | Animacja tekstury wody i eliminacja szwów | ✅ **ZREALIZOWANE** | `ShaderProgram.java`: uniform `uTime`, dynamiczne fale UV i słoneczny reflex specular w shaderze fragmentów. |
| **7** | Ciągłe niszczenie/stawianie bloków (Mouse Hold) | ✅ **ZREALIZOWANE** | `GameSettings.java`: `mouseHoldDelay = 0.22f` (Minecraft standard 220 ms), konfiguracja w menu Opcji. |
| **8** | Ekran wyboru świata bez podwójnego komunikatu | ✅ **ZREALIZOWANE** | `MainMenu.java`: dedykowany ekran `Select World` z tworzeniem i usuwaniem światów, podwójny komunikat wyeliminowany w `Main.java`. |
| **9** | F1 ukrywa HUD i obrys bloku | ✅ **ZREALIZOWANE** | `HudRenderer.java`: flaga `hideHud` wygasza HUD, celownik oraz selekcję bloku. |
| **12** | Przewijanie czatu i historia komend | ✅ **ZREALIZOWANE** | `ChatSystem.java`: bufor wierszy z ograniczeniem przewijania, strzałki góra/dół nawigują po historii komend oraz podpowiedziach. |
| **16** | Pełna lokalizacja JSON (I18n) i język angielski | ✅ **ZREALIZOWANE** | `I18n.java`: parser JSON bez zewnętrznych bibliotek, pliki `en_us.json` (domyślny) i `pl_pl.json`, obsługa menu, czatu i Debug HUD. |
| **17** | Eliminacja niebieskiej linii między blokami | ✅ **ZREALIZOWANE** | `TextureAtlas.java`: marginesy bezpieczeństwa (padding) i wcięcie UV zapobiegające krwawieniu sąsiednich kafelków atlasu. |
| **18** | Etykiety ON/OFF zamiast YES/NO w opcjach | ✅ **ZREALIZOWANE** | `OptionsMenu.java` & pliki językowe: przełączniki korzystają ze standaryzowanych kluczy `options.on` / `options.off`. |
| **19** | Auto-dopasowywanie tekstu w przyciskach | ✅ **ZREALIZOWANE** | `TextRenderer.java`: metoda `drawTextCenteredInBox(...)` dynamicznie skaluje tekst w dół z zachowaniem marginesów. |
| **21** | Zmiana przycisków cyklicznych na slidery w UI | ⏳ **OTWARTE** | Do zrobienia: suwaki ciągłe/dyskretne dla Render Distance, GUI Scale, Mouse Hold Delay, FOV, głośności dźwięków. |
| **22** | Przeliczanie layoutu GUI po zmianie rozmiaru okna | ⏳ **OTWARTE** | Do zrobienia: responsywny mechanizm kotwiczenia i wywołanie `layout()` w callbacku `glfwSetFramebufferSizeCallback`. |
| **23** | Assety bloków jako pliki PNG zamiast kodu | ⏳ **OTWARTE** | Do zrobienia: ładowanie plików PNG z `run/assets/textures/block/` przez `lwjgl-stb` i migracja na `GL_TEXTURE_2D_ARRAY`. |
| **24** | Globalna modułowa architektura rejestrów i obiektów gry | ⏳ **OTWARTE** | Do zrobienia: zastąpienie monolitycznych klas i enumów uogólnionym systemem rejestrów (`Registries.BLOCK`, `Registries.ITEM`, `Registries.ENTITY_TYPE`), hierarchią klas `Block`/`BlockState` oraz bazą pod moby (`Entity`, `LivingEntity`, `MobEntity`, `EntityType`). |
| **25** | Dynamiczny kolor trawy (Biome Colormap Tinting) | ⏳ **OTWARTE** | Do zrobienia: uśredniane próbkowanie klimatu biomów i tintowanie wierzchołków trawy zamiast wielu sztywnych bloków. |
| **26** | Zaśnieżony blok trawy (Snowy Grass Block) | ⏳ **OTWARTE** | Do zrobienia: wariant bloku lub stan `snowy = true` z dedykowaną teksturą boczną. |
| **27** | Zoptymalizowany system aktualizacji bloków | ⏳ **OTWARTE** | Do zrobienia: generyczny system powiadomień sąsiadów (`neighbour updates`) oraz kolejka planowanych aktualizacji (`scheduled ticks`). |

### 1.2 Istniejące zaawansowane podsystemy (nieuwzględnione w pierwotnej analizie)

1. **Wielowątkowy pipeline generowania i meshingu (Zero-GC):**
   - Asynchroniczna pula wątków roboczych `ExecutorService workerPool` (2–8 wątków CPU).
   - Generacja terenu (`Chunk.generateTerrain`) i budowa siatek wierzchołkowych (`ChunkMesher.buildChunkMesh`) działają w 100% na CPU bez wywołań OpenGL.
   - Eliminacja alokacji GC: `ThreadLocal<GeneratorContext>` i `ThreadLocal<MesherContext>` z płaskimi tablicami 1D indeksowanymi bitowo `(lx << 8) | (ly << 4) | lz`.
   - Główny wątek renderowania wgrywa bufory VAO/VBO na GPU z kolejki `readyMeshes` w ramach budżetu klatki (2.5 ms, do 64 siatek/klatkę).
   - Przepustowość: **2 000 – 5 600+ chunków/s** (komenda in-game `/bench`, live profiler w F3).
2. **Priorytetyzacja chunków według odległości i osi pionowej:**
   - Funkcja `calculateChunkPriorityScore(dx, dy, dz)` w `World.java`: poziom oczu (`dy = 0`) i gruntu (`dy = -1`) ma priorytet bezwzględny; jaskinie i chmury są odsuwane w tle.
3. **Dwuformatowy system zapisu świata z kompatybilnością wsteczną:**
   - **Format A (`.dat`, `VOX4`/`VOX5`):** Zapisuje wyłącznie różnice bloków (`modifiedBlocks`) w formacie bity-na-blok; miniaturowy rozmiar pliku (kilkadziesiąt KB).
   - **Format B (`.mca`, `VOXR` / `MCAV` v1):** Format regionów Anvil $32 \times 32$ kolumn chunków, kompresja ZLIB/Deflate (poziom 6, stopień upakowania 92–96%). Chunki pomijają generator szumu i ładują się natychmiast.
   - Atomowy zapis: plik tymczasowy `.tmp` + `Files.move(..., ATOMIC_MOVE)`.
4. **Rozbudowany UI & Debug System:**
   - Modułowa personalizacja panelu Debug HUD (`F3`) w 15 niezależnych elementach, zmiana stron lewa/prawa, kolejność góra/dół.
   - System wymuszeń reguł gry (Gamerule Overrides): czas świata, cykl dnia, latanie, noclip.
   - Autouzupełnianie i sugestie komend w czacie (klawisz `Tab`, nawigacja strzałkami).
   - Płynne przewijanie list (rolka myszy) w opcjach HUD, menu wyboru światów i ekwipunku.

---

## 2. Architektura docelowa i zasady konstrukcyjne

```
com.voxel
├── Main.java              // Pętla gry, inicjalizacja GLFW/OpenGL, okno
├── block                  // Docelowo: Block, BlockState, BlockRegistry, warianty bloków
├── config                 // GameSettings, DebugHudConfig, GameruleOverrides
├── entity                 // Player, kamera, fizyka gracza, bounding box AABB
├── i18n                   // Silnik tłumaczeń JSON (I18n), słowniki językowe
├── math                   // SimplexNoise, operatory wektorowe
├── render                 // Shadery, TextureArray, TextRenderer, Frustum Culling
├── ui                     // Ekrany (MainMenu, OptionsMenu, PauseMenu, Chat, Inventory)
├── util                   // GamePaths, Logger, CrashReporter
└── world
    ├── Chunk.java         // Struktura danych chunka 16x16x16, stan mesh/terrain
    ├── World.java         // Zarządzanie chunkami, raycast, pętla symulacji
    ├── WorldSaveManager   // Koordynator zapisu (.dat bity oraz .mca regiony)
    ├── RegionFileManager  // Niskopoziomowy I/O plików Anvil MCA (ZLIB Deflate)
    ├── gen                // TerrainGenerator, strefy klimatyczne, jaskinie 3D
    ├── mesh               // ChunkMesher, FloatList, culling ścian
    ├── tick               // [Docelowo] NeighbourUpdateDispatcher, ScheduledTickQueue
    └── fluid              // [Docelowo] Symulacja poziomów wody (1–7)
```

### Zasady architektoniczne:

1. **Jednokierunkowy przepływ zależności:** `core/math` ← `world` ← `render` ← `ui`. Świat nie ma pojęcia o OpenGL ani VAO/VBO.
2. **Świętość wątku graficznego (Render Thread):** Żadne operacje I/O dysku, kalkulacje szumów ani budowa wierzchołków CPU nie mogą blokować wątku renderującego. Upload do GPU jest limitowany budżetem czasu (2.5 ms na klatkę).
3. **Zero alokacji na stercie w pętli gorącej (Zero-GC Hot Path):** Wszelkie bufory robocze generowania i meshowania muszą korzystać ze struktur wielokrotnego użytku (`ThreadLocal` z płaskimi tablicami prymitywów).
4. **Paleta stanów per chunk:** Przy przejściu z numerycznych ID bloków na `BlockState`, zapis dyskowy zachowuje izolację formatu od identyfikatorów pamięciowych poprzez lokalną paletę stanu.
5. **Separacja symulacji od klatkarza:** Podział pętli na stały krok czasu symulacji (`worldTick`) oraz elastyczny `clientTick` i render z interpolacją.

---

## 3. Szczegółowe decyzje techniczne i specyfikacje systemów

### 3.1 Globalna modułowa architektura rejestrów i obiektów gry (Priorytet #24)

**Problem:** `BlockType.java` to monolityczny enum zawierający 42 typy bloków, mieszający właściwości fizyczne, identyfikatory, współrzędne atlasu i logikę specjalną. Podobnie brakuje jednolitej bazy pod moby, byty dynamiczne i przedmioty.

**Rozwiązanie (rozszerzenie na cały projekt):**
1. **Generyczny system rejestrów (`Registries`):**
   - Centralne rejestry oparte na unikalnych identyfikatorach tekstowych `Identifier` (`"voxel:zombie"`, `"voxel:dirt"`, `"voxel:apple"`):
     - `Registries.BLOCK`: instancje `Block`.
     - `Registries.ITEM`: instancje `Item`.
     - `Registries.ENTITY_TYPE`: typy bytów/mobów `EntityType<?>`.
   - Zapewnia to dynamiczne mapowanie dwukierunkowe: `Identifier <-> Instancja <-> numeryczny ID / State ID` dla zachowania szybkości pętli gry i kompaktowości zapisu na dysku.
2. **Architektura bloków:**
   - `Block`: abstrakcyjna klasa bazowa z metodami wirtualnymi (`isSolid()`, `isTransparent()`, `isCrossModel()`, `onNeighbourChanged()`, `onScheduledTick()`).
   - Podklasy specjalistyczne: `FullBlock`, `PlantBlock`, `FluidBlock`, `LeavesBlock`.
   - `BlockState`: niemutowalna kombinacja `(Block, StateProperties)` z prealokowanym `stateId`.
3. **Architektura encji i mobów (`Entity` / `MobEntity`):**
   - `Entity`: klasa bazowa dla wszelkich obiektów świata (pozycja, prędkość, rotacja, AABB dla kolizji z wokselami, `tick()`).
   - `LivingEntity`: parametry życiowe (zdrowie, obrażenia, animacja uderzenia `hurtTime`).
   - `MobEntity`: sztuczna inteligencja (`GoalSelector` z celami AI), pathfinding na siatce 3D bloków, nawigacja.
   - `EntityType<T>`: wzorzec fabryki `create(World)` rejestrowany w `Registries.ENTITY_TYPE`.
4. **Architektura przedmiotów:**
   - `Item`, `BlockItem`, `ItemStack(Item, count)` przygotowane pod inwentarz i crafting.

### 3.2 Dynamiczne tintowanie flory i zaśnieżona trawa (#25, #26)

- **Problem:** Obecnie istnieją sztywne bloki `GRASS` i `TAIGA_GRASS`, co uniemożliwia gładkie przejścia klimatyczne.
- **Implementacja tintowania:**
  - Jeden wspólny blok `GrassBlock`.
  - Podczas meshowania chunka (`ChunkMesher`), kolor wierzchołków górnej ściany trawy jest modyfikowany na podstawie temperatury i wilgotności biomu (`computeBiome(wx, wz)`).
  - Aby uniknąć ostrych krawędzi na granicach biomów, wartości koloru są uśredniane z próbek w promieniu 2 bloków (filtr 5x5).
- **Zaśnieżona trawa (`snowy`):**
  - Stan `BlockState` trawy zawierający właściwość boolean `snowy`.
  - Gdy blok powyżej to `SNOW` lub `ICE`, blok trawy przyjmuje stan `snowy = true`, który renderuje białą teksturę boczną z obwódką śniegu.

### 3.3 System aktualizacji bloków i scheduler (#27)

- **Mechanizm 1: Powiadomienia sąsiadów (`Neighbour Updates`):**
  - Gdy w `World.setBlock()` ulega zmianie blok na pozycji $(x, y, z)$, powiadamiane jest 6 bezpośrednich sąsiadów.
  - `PlantBlock.onNeighbourChanged()` bada, czy blok pod spodem (`y - 1`) jest lity. Jeśli podłoże zostało zniszczone, roślina natychmiast zamienia się w powietrze (i opcjonalnie upuszcza item).
- **Mechanizm 2: Planowane ticki (`Scheduled Ticks`):**
  - Kolejka priorytetowa `PriorityQueue<ScheduledBlockTick>` sortowana według docelowego numeru ticka świata.
  - Zastosowanie: opadanie piasku z opóźnieniem, rozlewanie się wody, topnienie lodu.
  - **Ochrona przed zamrożeniem wątku (Update Budget):** Maksymalny limit np. 256 aktualizacji na tick świata. Wszelkie nadmiarowe aktualizacje są przenoszone do następnego ticka.

### 3.4 Pipeline assetów PNG i Texture Array (#23)

- **Problem:** `TextureAtlas.java` generuje tekstury kafelków programowo w pamięci RAM procedurami bitowymi. Uniemożliwia to modowanie i tworzenie wysokiej jakości tekstur.
- **Rozwiązanie:**
  1. Wykorzystanie modułu `lwjgl-stb` do natywnego dekodowania plików PNG z katalogu `run/assets/textures/block/`.
  2. Zastąpienie atlasu 2D strukturą **`GL_TEXTURE_2D_ARRAY`**:
     - Każda tekstura o stałym wymiarze $16 \times 16$ pikseli stanowi osobną warstwę (layer) w tablicy tekstur.
     - Całkowita eliminacja texture bleedingu i szwów mipmap bez potrzeby sztucznego wcinania UV o pół teksela.
     - Warstwa tekstury jest przekazywana do shadera jako trzecia współrzędna UVW (`vec3 inTexCoord`).
  3. Obsługa braku pliku: w przypadku braku tekstury ładowana jest standardowa magenta-czarna szachownica z ostrzeżeniem w logu.

### 3.5 Framework UI i responsywność (#21, #22)

- **Dynamiczny layout przy resize (#22):**
  - Wszystkie ekrany (`MainMenu`, `OptionsMenu`, `PauseMenu`, `InventoryMenu`, `ChatSystem`) implementują interfejs z metodą `onResize(int newWidth, int newHeight, int guiScale)`.
  - Pozycje przycisków i paneli kalkulowane są dynamicznie z użyciem względnych kotwic (`Anchor.CENTER`, `Anchor.TOP_LEFT`, `Anchor.BOTTOM_RIGHT`) i marginesów.
- **Suwaki (Sliders, #21):**
  - Implementacja widgetu `Slider` z płynnym przeciąganiem myszą lub krokową regulacją dla:
    - Render Distance (2–16 chunków).
    - Mouse Hold Delay (100–400 ms).
    - Głośność (po dodaniu OpenAL).
    - FOV (kąt widzenia kamery).

---

## 4. Rzeczywista Roadmapa Silnika

Roadmapa została podzielona na logiczne fazy wynikające ze stanu faktycznego kodu i zależności systemowych.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ZREALIZOWANE (ZADANIA 1–20)                     │
│  Ocean Smoothstep, Góry i Śnieg Y=94, Pauza & Client/World Tick,      │
│  Kwiaty, Fizyka Piasku/Wody, Texture Fix, Auto-fit Text, Save Dat/MCA  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│              FAZA 1: Kluczowe zadania deweloperskie (#21–#27)          │
│                                                                        │
│  [1.1] Responsywne UI & Suwaki (#21, #22)                             │
│  [1.2] Pipeline PNG & Texture Array (`lwjgl-stb`) (#23)                │
│  [1.3] Architektura Block / BlockState / BlockRegistry (#24)           │
│  [1.4] Biome Colormap Tint trawy & Zaśnieżona trawa (#25, #26)         │
│  [1.5] System Aktualizacji Bloków (Neighbour updates & Ticks) (#27)   │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   FAZA 2: Rozszerzenia Świata i Silnika                │
│                                                                        │
│  - Rzeki i śródlądowe koryta wodne (RIVER_FREQ)                        │
│  - Trzcina cukrowa (Sugar Cane) przy wodzie                            │
│  - Wielopoziomowa płynna woda (poziomy 1–7 i model kaskadowy)          │
│  - Occlusion Culling (chunki zasłonięte terenem)                       │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    FAZA 3: Rozgrywka i Audio (Backlog)                 │
│                                                                        │
│  - System dźwiękowy (OpenAL: kroki, stawianie/niszczenie, woda)        │
│  - Ekwipunek z siatką craftingu 2x2 i stół 3x3                         │
│  - Płynne oświetlenie (Smooth Lighting BFS / Niebo + Światło Bloków)   │
│  - Profile Maven (Multiplatform: Linux/macOS/Windows)                  │
└────────────────────────────────────────────────────────────────────────┘
```

### Szczegółowy plan Fazy 1 (Zadania otwarte #21–#27):

#### Etap 1.1: UI & Responsywność (#21, #22)
- [ ] Dodanie klasy `Slider` dziedziczącej po elemencie bazowym UI.
- [ ] Zastąpienie przycisków cyklicznych suwakami w `OptionsMenu.java`.
- [ ] Podpięcie callbacku `glfwSetFramebufferSizeCallback` pod hierarchię ekranów i przeliczanie wymiarów UI na żywo.

#### Etap 1.2: Pipeline PNG & Texture Array (#23)
- [ ] Dodanie zależności `org.lwjgl:lwjgl-stb` do `pom.xml`.
- [ ] Utworzenie katalogu `run/assets/textures/block/` i wyodrębnienie tekstur do pojedynczych plików PNG.
- [ ] Zaimplementowanie `TextureArrayLoader` ładującego tekstury do `GL_TEXTURE_2D_ARRAY`.
- [ ] Aktualizacja shadera fragmentów (`uTextureArray` zamiast atlasu 2D).

#### Etap 1.3: Globalna architektura rejestrów, bloków, itemów i mobów (#24)
- [ ] Utworzenie generycznego systemu rejestracji `Registries` (`BLOCK`, `ITEM`, `ENTITY_TYPE`).
- [ ] Refaktoryzacja bloków: klasy `Block`, `BlockState`, podklasy (`FullBlock`, `PlantBlock`, etc.).
- [ ] Wdrożenie fundamentu encji i mobów: klasy `Entity`, `LivingEntity`, `MobEntity`, `EntityType`.
- [ ] Wdrożenie struktury przedmiotów: `Item`, `ItemStack`.
- [ ] Zachowanie zgodności wstecznej numerycznych ID dla formatu zapisu `VOX4` i `VOXR`.

#### Etap 1.4: Dynamiczny Kolor Trawy i Śnieg (#25, #26)
- [ ] Wprowadzenie palety kolorów flory indeksowanej temperaturą i wilgotnością klimatu.
- [ ] Uśrednianie koloru trawy w `ChunkMesher` z sąsiednich współrzędnych.
- [ ] Dodanie stanu trawy `snowy = true` z białą teksturą górną i ośnieżonym bokiem.

#### Etap 1.5: System Aktualizacji Bloków (#27)
- [ ] Wdrożenie `World.notifyNeighbours(x, y, z)`.
- [ ] Sprawdzanie podparcia dla kwiatów (`ROSE`, `DANDELION`, etc.) i natychmiastowe niszczenie przy braku podłoża.
- [ ] Wdrożenie kolejki `ScheduledTickQueue` z budżetem operacji na klatkę.

---

## 5. Dokumentacja powiązana i źródła prawdy

W celu zachowania przejrzystości, każdy dokument w repozytorium pełni ściśle określoną rolę:

1. **`Java Voxel 3D — Architektura, Systemy i Roadmapa.md` (niniejszy plik):**
   Główny manifest architektoniczny, aktualny stan całego projektu, zestawienie systemów i priorytetyzacja prac.
2. **`docs/WORLD_GENERATION.md`:**
   Techniczna specyfikacja parametrów szumów Simplex, wzorów wysokości, stref klimatycznych, mikro-ditheru i reguł plaż.
3. **`docs/REGION_STORAGE_FORMAT.md`:**
   Binarna specyfikacja formatów zapisu `.dat` (bity różnicowe) oraz `.mca` (skompresowane regiony Anvil ZLIB/Deflate).
4. **`docs/TODO.md`:**
   Szczegółowy rejestr zadań technicznych silnika z podziałem na podsystemy.
5. **`docs/developer todo.md`:**
   Pierwotna lista wytycznych deweloperskich (pozycje 1–20 ukończone, 21–27 w realizacji).

---

## 6. Kryteria Jakościowe i Definicje Ukończenia (DoD)

- **Brak regresji istniejących funkcji:** Wszystkie testy automatyczne w `src/test/java/` (`DeveloperFeaturesTest`, `FeatureSetVerification`, `GuiScaleAndScrollTest`) muszą przechodzić w 100% (kod wyjścia 0).
- **Zero-GC:** Nowe implementacje nie mogą alokować obiektów tymczasowych w pętli generacji terenu i budowy siatek.
- **Atomowość danych:** Zmiany w strukturze bloków (`BlockState`) nie mogą uszkadzać istniejących zapisów w folderze `run/saves/` (wymagana obsługa palety i kompatybilności wstecznej).
- **Stabilność wątkowa:** Wszystkie modyfikacje graficzne i wywołania biblioteki OpenGL muszą pozostać ściśle na głównym wątku renderującym.
