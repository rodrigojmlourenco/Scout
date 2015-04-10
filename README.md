# Scout
CycleOurCity goes Mobile - Scout


## TODO

- [ ] Tornar assíncrono o processo de arquivação
- [ ] Estudar melhor como pode ser realizada a extracção da aceleração linear.
	- [ ] Low-pass Filter vs High-pass Filter vs No-Filter
- [ ] Estudar se compensa fazer merge das amostras de localição durante o processo de extracção de features.
	- [ ] Obter declive
	- [ ] Estudar qual a mais precisa a velocidade obtida pelo GPS ou calculada a partir das coordenadas
- [ ] Criação de um classificador capaz de distinguir entre dispositivo estacionario e em movimento.
	- [ ] Criar|Procurar script que transforme amostras `.json` em formato `.arff`
	- [ ] Criar uma base de dados classificada, para treino do classificador
- [ ] Documentar as classes criadas

## Funf-OpenSensing Schedueling CheatSheet

* `"duration":0` : com a duração da tarefa de uma probe definida a 0 então essa tarefa é iniciada mas nunca terminada.
* `"strict":true`: garante que a tarefa de uma probe é realizada de acordo com a especificação do `interval`
* `"offset":0`	 : força a que a tarefa da probe se realize imediatamente.

## Open Issues

### ISSUE 07043:EmptyDB
Quando iniciada a acção de arquivação, caso a DB não se encontre populada a aplicação crasha. Apesar da correcção realizada a aplicação crasha esporadicamente quando iniciada uma acção de arquivação. Tanto quanto consigo perceber este problema pode ocorrer devido a condições de corrida violadas.
A excepção lançada é a seguinte:
```
java.lang.NullPointerException: Attempt to invoke interface method 'boolean edu.mit.media.funf.storage.FileArchive.add(java.io.File)' on a null object reference
            at pt.ulisboa.tecnico.cycleourcity.scout.storage.archive.ScoutArchive.add(ScoutArchive.java:89)
            at pt.ulisboa.tecnico.cycleourcity.scout.storage.archive.ScoutArchive.add(ScoutArchive.java:94)
            at pt.ulisboa.tecnico.cycleourcity.scout.storage.ScoutStorageManager.archive(ScoutStorageManager.java:131)
            at pt.ulisboa.tecnico.cycleourcity.scout.mobilesensing.MobileSensingPipeline.archiveData(MobileSensingPipeline.java:213)
            at pt.ulisboa.tecnico.cycleourcity.scout.pipeline.ScoutPipeline.onRun(ScoutPipeline.java:50)
            at pt.ulisboa.tecnico.cycleourcity.scout.MainActivity$2.onClick(MainActivity.java:167)
```

### ISSUE 04102:VoidMotionSignals
Estão a ser armazenadas amostras de sensores de movimento vazios no `AccelerometerPipeline`. Este problema deve-se a um bug na executação da `FeatureExtractionStage` onde as features estão a ser construídas independentemente da existência ou não de amostras.

### ISSUE 04104:LazyApp
Com o passar do tempo a aplicação vai perdendo performance. Mesmo sem uma sessão de sensing iniciada a aplicação está a consumir muitos recursos (GC constantemente a ser chamado). É assim necessário realizar um estudo de quais os possíveis pontos de optimização.

## Recently Solved Issues

### ISSUE 07041:Scheduler
O problema foi "resolvido" através da especificação do valor da duração das tarefas das probes como sendo 0 (`"duration":0`). Embora isto funcione bem para a `AccelerometerSensorProbe` e para a `GravitySensorProbe`, a sampling rate da `LocationProbe` ainda deixa a desejar.

### ISSUE 04103:UnexpectedCrash
Por vezes, ao iniciar pela primeira vez uma sessão de sensing, a aplicação crasha de forma inesperada. Após a análise do BUG report, gerado pelo dispositivo, foram identificados várias excepções fatais causadas por uma NullPointerException. Esta é sempre causada em `LocationPipeline.java:188`, quando é chamado o método `JsonElement.getAsString` sobre um objecto não existente. Esta excepção ocorre quando o provider da localização é a rede e o método acede ao JsonElemente "extras", que por sua vez é não existente.

A solução para este problema passa assim por verificar se o campo "extras" existe ou não e apenas caso este exista são adicionados os campos especiais à nova amostra.

### ~~ISSUE 07043:EmptyDB~~