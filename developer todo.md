# Developer TODO (Priorytetowe)

- [x] 1. napraw ocean aby schodzil glebokosc schodzila bardziej lagodnie (Exponentialy, blisko brzego powoli coraz plycej, dalej od brzegu = prawie natychmiastowy upadek)
- [x] 2. dodaj drzewa tajgowe do gór (może być jako mikro biom), rowniez tych śnieznych ale rzadziej, przy czym gore śnieżne powinny znajdowac sie o wiele wyzej
- [x] 3. napraw nie-pauzowanie czasu przy wstrzymanej grze
- [x] 4. dodaj wiecej rodzajow kwiatow
- [x] 5. fizyka wody oraz innych blokow, przy czym zachowanie wody powinno byc dostepne pod komenda /worldsetting gdzie zmieniac mozna pomiedzy minecraftowym a terrariowym
- [x] 6. popraw teksture wody, aby wydawala sie mniej static, w koncu ocean zawsze sie porusza
- [x] 7. dodaj obsluge przytrzymywania przyciskow na myszce, z delayem customizable tylko w opcjach gry, z domyslnym tym znanym z minecraft
- [x] 8. po wczytaniu swiata informacja o zaladowanym swiecie pojawia sie na chacie dwa razy, pozbadz sie opcji play singleplayer na rzecz wybierz swiat, i wlasnie tam na wzor minecrafta jest opcja usuwania / tworzenia nowych swiatow

- [x] 9. f1 powinno ukrywac blok outline
- [x] 10. brzeg przy oceanach ma nagly spadek 2 blokow, cel wszystkich poprawek z oceanem byl taki aby przy brzegu nie bylo zadnego spadku powyzej 1 bloku
- [x] 11. zmniejsz odleglosc przed pierwszym upadkiem na dwa bloki glebokosci oceanu o polowe
- [x] 12. przy chacie uzywajac up/down mozna wyjsc poza widoczne na ekranie elementy, spraw aby dzialalo to tak jak w minecraft
- [x] 13. w /worldsettings mowiac fizyka wody z minecraft chodzilo mi o tą nowoczesna, ale tą z preclassic mozesz zostawic pod innym world settings. Na potrzeby fixa dodaj kilka stanow wody jak w minecraft
- [x] 14. woda nie jest "continous", kiedy umiescimy dwa bloki obok siebie widocznie widac ucieta teksture
- [x] 15. woda powinna niszczyc dekoracyjne elementy (trawa, kwiaty)
- [x] 16. nie cala gra jest localized. domyslnym jezykiem powinien byc angielski - dotyczy to np. konsoli, debug menu jest w polowie po polsku pomimo wybranego jezyka angielskieg
- [x] 17. pomiedzy blokami jest ~1 pikselowa linia niebieskiego
- [x] 18. zamiast YES/NO uzywaj ON/OFF w ustawieniach, ktore rowniez maja niektore teksty unlocalized
- [x] 19. "Play selected w" to wciaz niemieszczacy sie w przycisku tekst. nie naprawiaj problemu dla dla tylko tego przycisku, azmiasdt tego zrob globalny fix
- [x] 20. Czas nie zatrzymuje sie przy odpaleniu chatu, ale movement gracza sie zatrzymuje. Rozdziel gre na dwa ticki: client tick oraz world tick

- [x] 21. zmien cycle buttons na slidery
- [x] 22. gui powinno byc kalkulowane jeszcze raz po zmienieniu wielkosci okna
- [ ] 23. assety zamiast generowac dynamicznie w kodzie powinny byc przetrzymywane jako png
- [ ] 24. modularnosc kodu calego projektu !!! zastap monolityczne klasy/enumy globalnym obiektowym systemem rejestrow (rejestry blokow, itemow oraz encji/mobow w stylu minecrafta z klasami bazowymi Entity, LivingEntity, Mob, Block, BlockState, zamiast trzymac wszystko w pojedynczych klasach)
- [ ] 25. grass block powinien miec mozliwosc latwego alterowania koloru trawy, dla plynnych przejsc miedzy biomami, wtedy duzo rodzajow trawy mozna by zastapic jedna wspolna
- [ ] 26. dodaj osniezony grass block
- [ ] 27. wprowadz system block update czy cos podobnego do zoptymalizowanego sprawdzania czy np. blok pod kwiatem wlasnie zostal zniszczony
