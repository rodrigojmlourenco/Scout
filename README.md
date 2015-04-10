# Scout
CycleOurCity goes Mobile - Scout


## Funf-OpenSensing Schedueling CheatSheet

* `"duration":0` : com a duração da tarefa de uma probe definida a 0 então essa tarefa é iniciada mas nunca terminada.
* `"strict":true`: garante que a tarefa de uma probe é realizada de acordo com a especificação do `interval`
* `"offset":0`	 : força a que a tarefa da probe se realize imediatamente.

## Open Issues

### ISSUE 07043:EmptyDB
Quando iniciada a acção de arquivação, caso a DB não se encontre populada a aplicação crasha.

### ISSUE 04102:VoidMotionSignals
Estão a ser armazenadas amostras de sensores de movimento vazios no `AccelerometerPipeline`. Este problema deve-se a um bug na executação da `FeatureExtractionStage` onde as features estão a ser construídas independentemente da existência ou não de amostras.

## Recently Solved Issues

### ISSUE 07041:Scheduler
** BANDAGE **
Desde a actualização, do dispositivo de testes, para Android 5.1, a especificação customizada do schedueling das diferentes probes (especificado no ficheiro `/res/values/string.xml`) não está a funcionar correctamente. Em particular as probes executam-se com uma sampling rate de maior frequência caso não seja especificado os seus intervalos.

O problema foi "resolvido" através da especificação do valor da duração das tarefas das probes como sendo 0 (`"duration":0`). Embora isto funcione bem para a `AccelerometerSensorProbe` e para a `GravitySensorProbe`, a sampling rate da `LocationProbe` ainda deixa a desejar.

### ~~ISSUE 07043:EmptyDB~~



