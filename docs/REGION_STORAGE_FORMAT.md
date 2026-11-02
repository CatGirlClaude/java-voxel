# Architektura Zapisu Świata i Format Regionów (Region Storage / MCA)

> **Dokumentacja techniczna formatów zapisu stanu świata silnika Java Voxel 3D dla programistów i agentów AI.**

---

## 1. Dwa Formaty Zapisu Świata

W grze dostępne są dwa niezależne, w pełni kompatybilne formaty zapisu, przełączane w menu opcji gry (**ESC -> Opcje -> System i Zapis -> Format zapisu świata**):

| Format | Rozszerzenie | Nagłówek Magic | Kompresja | Kiedy stosować? |
|---|---|---|---|---|
| **Opcja A: Różnice bloków (Domyślny)** | `saves/<nazwa>.dat` | `0x564F5834` (`VOX4`) | Format binarny bity-na-blok | Standardowa rozgrywka, minimalny rozmiar plików (kilkadziesiąt KB). |
| **Opcja B: Format Regionów (MCA)** | `saves/<nazwa>.dat` + `saves/<nazwa>_regions/r.<rx>.<rz>.mca` | `0x564F5852` (`VOXR`) | ZLIB / Deflate (poziom 6) | Duże budowle, edycje terenu na wielką skalę, natychmiastowe ładowanie bez szumu. |

---

## 2. Format A: Różnice bloków (`.dat`)

### Struktura pliku `saves/<nazwa>.dat`:
```text
[Magic: 4B int] (0x564F5834)
[Seed: 8B long]
[TimeOfDay: 4B float]
[Player X, Y, Z: 3x 8B double]
[Player Yaw, Pitch: 2x 4B float]
[Player Flying: 1B boolean]
[SelectedSlot: 4B int]
[Hotbar 9 slots: 9x 1B byte]
[ModifiedBlocksCount: 4B int]
  -> Dla każdego bloku:
     [Block X: 8B long]
     [Block Y: 8B long]
     [Block Z: 8B long]
     [BlockType ID: 1B byte]
```

---

## 3. Format B: Format Regionów (`.mca`)

Struktura wzorowana na formacie Minecraft Anvil (MCA), zaadaptowana do trójwymiarowych chunków sześciennych $16 \times 16 \times 16$ (4096 bloków na chunk).

### Koordynaty regionu
Dla chunka o współrzędnych `(cx, cy, cz)`:
$$\text{rx} = \lfloor \text{cx} / 32 \rfloor, \quad \text{rz} = \lfloor \text{cz} / 32 \rfloor$$

Każdy plik regionu `r.<rx>.<rz>.mca` reprezentuje obszar poziomy $32 \times 32$ kolumn chunków ($512 \times 512$ bloków).

### Struktura pliku `r.<rx>.<rz>.mca`:
```text
[Header Magic: 4B int] (0x4D434156 -> "MCAV")
[Version: 4B int] (1)
[ChunkCount: 4B int]
  -> Dla każdego chunka w pliku regionu:
     [ChunkX: 8B long]
     [ChunkY: 8B long]
     [ChunkZ: 8B long]
     [UncompressedSize: 4B int] (zawsze 4096 bajtów)
     [CompressedSize: 4B int]
     [CompressedPayload: N bajtów] (strumień ZLIB / Deflate)
```

### Zalety i specyfikacja:
1. **Wysoki stopień upakowania:** 4096 bajtów bloku kompresuje się do ok. 150-350 bajtów (kompresja rzędu 92-96%).
2. **Bezpieczny zapis (Atomic Move):** Zapis odbywa się do pliku `.tmp`, a po pomyślnym zrzucie następuje atomowe zastąpienie pliku docelowego `Files.move(..., ATOMIC_MOVE)`, co zapobiega uszkodzeniu save'a przy nagłym zamknięciu gry.
3. **Pominięcie generatora szumu:** Przy ładowaniu chunka z pliku regionu metoda `chunk.setRawBlocks(...)` pomija wywołanie `TerrainGenerator.generateChunkTerrain(this, noise)`, co radykalnie przyspiesza streaming terenu w obszarach zbudowanych przez gracza.

---

## 4. Kompatybilność Wsteczna

Metoda `WorldSaveManager.loadWorld()` automatycznie rozpoznaje nagłówek magic:
- `0x564F5831` (VOX1) – starszy format z kompresją bitową współrzędnych.
- `0x564F5832` (VOX2) – format ze zmiennymi 64-bitowymi X/Z i 32-bitowym Y.
- `0x564F5833` (VOX3) – pełne 64-bitowe współrzędne 3D gracza i bloków.
- `0x564F5834` (VOX4) – zapis pozycji slotów hotbara (9 bloków).
- `0x564F5852` (VOXR) – format regionów MCA z powiązanym katalogiem `_regions/`.

Gracz może w każdej chwili przełączyć format zapisu w menu opcji bez obawy o utratę postępów.
