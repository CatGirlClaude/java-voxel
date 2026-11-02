# Dokumentacja Projektu Java Voxel 3D

Wszystkie dokumenty techniczne, specyfikacje i plany rozwoju projektu znajdują się w tym katalogu. Jest to **Baza Wiedzy Silnika (Context Hub)** przygotowana dla agentów AI oraz deweloperów, aby umożliwić natychmiastowe zapoznanie się z architekturą bez potrzeby przeszukiwania całego kodu źródłowego:

- **[Przewodnik Operacyjny Agenta (`AGENT_GUIDELINES.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/AGENT_GUIDELINES.md)**:
  Nadrzędny przewodnik inżynieryjny dla modeli AI: workflow użytkownik-agent, zasady nienaruszalne (Zero-GC, wątek renderujący OpenGL, Cubic Chunks), komendy CLI oraz instrukcje rozszerzania silnika.
- **[Główny Manifest Architektoniczny i Roadmapa (`ARCHITECTURE.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/ARCHITECTURE.md)**:
  Nadrzędny opis architektury silnika, stan implementacji, kluczowe zasady projektowe oraz docelowa mapa drogowa.
- **[Zadania Techniczne i Refinowane TODO (`TODO.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/TODO.md)**:
  Oficjalny rejestr techniczny silnika: zrealizowane optymalizacje, uszczegółowione specyfikacje zadań z notatnika dewelopera (#21–#27) oraz backlog silnika.
- **[Generator Świata i Architektura Terenu (`WORLD_GENERATION.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/WORLD_GENERATION.md)**:
  Szczegółowa specyfikacja algorytmów szumu Simplex, wzorów wysokości, stref klimatycznych, mikro-ditheru, rzeźbienia jaskiń i reguł plaż.
- **[System Zapisu Świata i Format Regionów MCA (`REGION_STORAGE_FORMAT.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/REGION_STORAGE_FORMAT.md)**:
  Binarna specyfikacja formatów zapisu (`.dat` bity-na-blok oraz `.mca` skompresowane regiony Anvil ZLIB/Deflate), nagłówki `VOX1`–`VOXR` oraz atomowość zapisu.
- **[Notatnik Dewelopera (`developer todo.md`)](file:///c:/Users/september/Desktop/ja/antigravity/java%20voxel/docs/developer%20todo.md)**:
  Szybki notatnik pomysłów użytkownika. Zadania z tego pliku są regularnie analizowane, refinowane do poziomu specyfikacji technicznej i przenoszone do `TODO.md`.
