# Scout
CycleOurCity goes Mobile - Scout


## Open Issues

### ISSUE 07041:Scheduler ** URGENT **
Desde a actualização, do dispositivo de testes, para Android 5.1, a especificação customizada do schedueling das diferentes probes (especificado no ficheiro `/res/values/string.xml`) não está a funcionar correctamente. Em particular as probes executam-se com uma sampling rate de maior frequência caso não seja especificado os seus intervalos.

Este é um problema crítico por o schedueling nativo não vai completamente de acordo com as necessidades da aplicação.


## ISSUE 07043:EmptyDB
Quando iniciada a acção de arquivação, caso a DB não se encontre populada a aplicação crasha.

