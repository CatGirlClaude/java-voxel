# Architektura i Zasady Generatora Świata (World Generation Guide)

> **Dokumentacja techniczna dla agentów i deweloperów silnika voxelowego.**
> Zawiera zasady, stałe matematyczne, architekturę pipeline'u generowania terenu oraz wytyczne zapobiegające powrotowi znanych błędów (np. wysychanie oceanów, wielkie kliny piasku, zderzenia klimatyczne).

---

## 1. Główne Założenia i Stałe Świata

- **Układ współrzędnych:** Prawoskrętny układ 3D. 1 jednostka = 1 blok ($1\text{m}$).
- **Cubic Chunks:** Świat podzielony jest na trójwymiarowe sześciany o wymiarach **$16 \times 16 \times 16$ bloków** (`Chunk.SIZE_X = 16`, `SIZE_Y = 16`, `SIZE_Z = 16`).
- **Poziom morza (`SEA_LEVEL`):** Stała wartość **`28`**.
  - Woda generuje się **wyłącznie**, gdy `wy > height && wy <= SEA_LEVEL`.
  - Jeśli powierzchnia ma `height == 28`, woda ma grubość $0$ bloków (suchy ląd). Dlatego dno basenów wodnych musi zawsze mieć `height <= 27`.
- **Częstotliwości szumów (SimplexNoise):**
  - Kontynentalność (Ocean vs Ląd): `OCEAN_FREQ = 0.0009` (bardzo szerokie baseny i kontynenty).
  - Klimat (Temperatura / Wilgotność): `CLIMATE_FREQ = 0.00075`.
  - Pasma górskie: `MOUNTAIN_FREQ = 0.0014`.

---

## 2. Pipeline Generowania Terenu (`TerrainGenerator.java`)

Generowanie chunka odbywa się w ścisłej kolejności faz:

```
[1. Siatka 2D: computeSurfaceHeight & computeBiome]
                        │
                        ▼
[2. Rzeźbienie Jaskiń 3D (Trilinear Simplex Cave Grid)]
                        │
                        ▼
[3. Wypełnianie Blokami: selectBiomeTerrainBlock & Woda/Lód]
                        │
                        ▼
[4. Generowanie Drzew i Dużej Flory (Siatka komórek 5x5)]
                        │
                        ▼
[5. Naturalna Roślinność Powierzchniowa (Tall Grass & Kwiaty)]
```

---

## 3. Matematyka Wysokości Powierzchni (`computeSurfaceHeight`)

### A. Baseny Oceaniczne (`oceanContinental < -0.05`)
- Dno oceanu **nigdy nie może dryfować na poziomie morza**.
- Głęboki ocean (`cont < -0.22`): dno na `Y = 6..12` (głębokość wody $16-22$ bloki).
- Szelf oceaniczny (`-0.22 <= cont < -0.05`): woda ma głębokość minimum $3.5-8$ bloków aż do samej strefy przybrzeżnej.

### B. Wynurzenie Lądu (`-0.05 <= oceanContinental <= 0.01`)
- Teren gwałtownie, ale płynnie (`smoothstep`) wyłania się z wody od poziomu `SEA_LEVEL - 3.5` do krawędzi brzegu `SEA_LEVEL + 0.9`.
- **Zapobiega to powstawaniu 200-metrowych wyschniętych płycizn.**

### C. Elewacja Śródlądowa (`oceanContinental > 0.01`)
- Bazowa wysokość lądu śródlądowego wynosi `SEA_LEVEL + 4.8 + continental * 16.0 + ...`.
- Gwarantuje to, że doliny wewnątrz równin i lasów nie opadają przypadkowo do `Y = 28`, co zapobiega powstawaniu wyschniętych niecek.

### D. Anti-Terracing Micro-Dither
- Na bardzo łagodnych zboczach zaokrąglenie `Math.round()` tworzyło nienaturalne, okrągłe tarasy schodkowe.
- Rozwiązanie: dodano mikro-szum o wysokiej częstotliwości:
  ```java
  double microDither = noise.noise2D(wx * 0.15 + 43.0, wz * 0.15 - 87.0) * 0.35;
  return (int) Math.round(baseElevation + microDither);
  ```
  Rozbija on krawędzie schodków na organiczne, pofalowane zbocza.

---

## 4. Zasady Biomów Przybrzeżnych i Plaż (`isNearOceanWater`)

> [!IMPORTANT]
> **Plaża w Minecrafcie to biom mikro-krawędziowy (2–4 bloki szerokości), a NIE strefa szumu kontynentalnego.**

### Algorytm detekcji plaży:
1. **Ograniczenie strefy kontynentalnej:** `cont >= -0.055 && cont <= -0.010`.
2. **Wysokość:** Wyłącznie wąskie pasmo tuż nad wodą: `hC >= SEA_LEVEL && hC <= SEA_LEVEL + 2`.
3. **Prawdziwa woda morska obok:** Próbnik bada odległość $1, 2, 3$ bloków we wszystkich kierunkach. Plaża powstaje **tylko wtedy**, gdy obok znajduje się komórka spełniająca `isOceanWaterAt`:
   - `sampleContinental < -0.045` (należy do basenu oceanicznego),
   - `computeSurfaceHeight < SEA_LEVEL` (jest pod taflą wody).
4. Jeśli w odległości 3 bloków nie ma wody morskiej, blok **zawsze** przyjmuje biom rodzimy klimatu (Plains, Forest, Jungle, Taiga).

---

## 5. Separacja Klimatyczna Wybrzeży i Dithering

Nigdy nie należy stawiać żółtego piasku w zimnych strefach klimatycznych:

| Strefa klimatyczna | Temperatura ($T$) | Typ wybrzeża | Główne bloki powierzchniowe |
| :--- | :--- | :--- | :--- |
| **Arktyczna (Polarna)** | $T < -0.16$ | `SNOWY_TUNDRA` (Wybrzeże arktyczne) | Lód szelfowy (`ICE`), Śnieg (`SNOW`), Zmarzlina (`COBBLESTONE`) |
| **Północna (Chłodna)** | $-0.16 \le T < -0.06$ | `TAIGA` (Kamienisty brzeg / Stony Shore) | Otoczaki (`COBBLESTONE`), Skała (`STONE`), kępy `TAIGA_GRASS` |
| **Umiarkowana / Ciepła** | $T \ge -0.06$ | `BEACH` (Złota plaża) | Piasek (`SAND`), otoczaki przy wodzie, trawy wydmowe |

### Organiczny Dithering Granic Biomu:
- Na styku plaży i trawiastego lądu bloki są mieszane przy użyciu hasha współrzędnych:
  - Na plaży przy `height >= SEA_LEVEL + 2`: 37.5% bloków to kępy trawy (`GRASS`).
  - Na lądzie przy `height <= SEA_LEVEL + 2`: 12.5% bloków to piasek (`SAND`).
- **Eliminacja brudnych ścianek (DIRT on scarps):** Jeśli blok trawy sąsiaduje z plażą i ma uskok 1 bloku w dół, jego warstwa podpowierzchniowa (`wy = height - 1`) to `SAND`, a głębiej `SANDSTONE`. Dzięki temu pionowa ścianka wygląda jak piaszczysta wydma, a nie surowa ziemia.

---

## 6. Zasady Roślinności i Flory

1. **Drzewa na plaży:** Drzewa leśne i dżunglowe mają kategoryczny zakaz generowania się na plaży (`biome != Biome.BEACH`).
2. **Flora plażowa:** Na plaży dopuszczone są wyłącznie rzadkie kępy traw wydmowych (`TALL_GRASS`, szansa ok. 5%).
3. **Bezpieczeństwo jaskiń:** Funkcja `isSurfaceCarvedByCave` sprawdza rzeźbienie w promieniu pnia, zapobiegając lewitowaniu koron drzew nad wejściami do jaskiń.

---

## 7. Optymalizacja Renderowania i Wytyczne Wydajnościowe

- **Frustum Culling (JOML SIMD):**
  W klasie [`World.java`](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/src/main/java/com/voxel/world/World.java) pętla `renderChunks` testuje AABB każdego chunka z użyciem `FrustumIntersection.testAab()`.
  Chunki za plecami gracza lub poza kadrem są natychmiast odrzucane (oszczędność 75%–95% draw calli).
- **Empty Mesh Culling:**
  Chunki głęboko pod ziemią lub w powietrzu bez geometrii są natychmiast pomijane za pomocą `c.hasAnyMesh()`.
- **Zero-Allocation Transparent Pass:**
  Tylko chunki posiadające geometrię wody/szkła/lodu trafiają do listy `visibleTransparent` i są sortowane `back-to-front`.

---

## 8. Wielowątkowa Generacja, Zero-GC Pipeline i In-Game Benchmark

### A. Architektura Wielowątkowa (`ExecutorService workerPool`)
- Generacja terenu (`Chunk.generateTerrain`) oraz budowa siatek wierzchołków (`ChunkMesher.buildChunkMesh`) wykonywane są asynchronicznie w tle przez pulę wątków roboczych (`Voxel-Worker-X`, 2-8 wątków zależnie od liczby rdzeni CPU).
- Wątki robocze operują **wyłącznie na pamięci CPU** (`FloatList`), nie wykonując żadnych wywołań biblioteki OpenGL.
- Główny wątek renderowania (render thread) pobiera gotowe siatki z wątkowo-bezpiecznej kolejki `readyMeshes` i wgrywa bufory VAO/VBO na GPU (`uploadMeshes`) w ramach budżetu czasu klatki (2.5 ms, do 64 siatek/klatkę).

### B. Konteksty Zero-GC (`ThreadLocal`)
- Zastosowano `ThreadLocal<GeneratorContext>` oraz `ThreadLocal<MesherContext>` z płaskimi tablicami 1D indeksowanymi bitowo (`(lx << 8) | (ly << 4) | lz`).
- **Zero alokacji obiektów na stercie (zero GC pressure):** eliminacja ponad 400 tablic `double[][][]` i `boolean[][][]` tworzonych dawniej dla każdego generowanego chunka.
- Pamięć podręczna kolumny wewnątrz wątku (`lastColX`, `lastColZ`) eliminuje powtórne przeliczanie szumów 2D wysokości i biomów dla kolejnych pionowych chunków w tej samej kolumnie.
- Zoptymalizowano `SimplexNoise` funkcją szybkiego zaokrąglania `fastFloor()`.

### C. In-Game Benchmark i Live Profiler
- **Komenda `/bench` (lub `/benchmark [ilość_chunków]`, domyślnie 512):**
  Uruchamia w tle asynchroniczny test syntetyczny na puli wątków, mierząc osobno czas generacji terenu, budowy siatek, łączną przepustowość w `ch/s` oraz wystawia ocenę wydajności (`A+ / A / B / C`).
- **Karta wyników (HUD Card):** Po wykonaniu benchmarku na ekranie pojawia się estetyczna karta z podsumowaniem czasu i przepustowości przez 14 sekund.
- **Komenda `/bench live`:** Przełącza ciągły overlay z bieżącymi metrykami wydajności na ekranie.
- **Panel debugowania F3:** Wyświetla średnie czasy `Gen / Mesh / GPU`, rozmiary kolejek oraz przepustowość potencjalną w czasie rzeczywistym.
