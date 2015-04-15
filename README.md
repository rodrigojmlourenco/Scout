# Scout
CycleOurCity goes Mobile - Scout

<img src="./img/scout_screenshot.png"/>

## TODO


- [ ] Estudar como optimizar a precisão da elevação capturada pelo GPS.
- [ ] Estudar melhor como pode ser realizada a extracção da aceleração linear.
      - [ ] Low-pass Filter vs High-pass Filter vs No-Filter
- [ ] Criação de um classificador capaz de distinguir entre dispositivo estacionario e em movimento.
      - [ ] Criar|Procurar script que transforme amostras `.json` em formato `.arff`
      - [ ] Criar uma base de dados classificada, para treino do classificador
- [ ] Documentar as classes criadas
- [x] Criar módulo para criação de ficheiros GPX
- [x] Estudar o problema da segmentação de sequências contínuas de sinais.
- [x] Tornar assíncrono o processo de arquivação (Desnecessário)
- [x] Extracção de features no `LocationPipeline`
      - [x] Fazer merge de amostras de localização "relacionadas"
      - [x] Obter distância viajada
	- [x] Obter declive (*Nota:* muito impreciso)



## Funf-OpenSensing Schedueling CheatSheet

* `"duration":0` : com a duração da tarefa de uma probe definida a 0 então essa tarefa é iniciada mas nunca terminada.
* `"strict":true`: garante que a tarefa de uma probe é realizada de acordo com a especificação do `interval`
* `"offset":0`	 : força a que a tarefa da probe se realize imediatamente.

## Open Issues

### ISSUE 04102:VoidMotionSignals
Estão a ser armazenadas amostras de sensores de movimento vazios no `AccelerometerPipeline`. Este problema deve-se a um bug na executação da `FeatureExtractionStage` onde as features estão a ser construídas independentemente da existência ou não de amostras.


Com o passar do tempo a aplicação vai perdendo performance. Mesmo sem uma sessão de sensing iniciada a aplicação está a consumir muitos recursos (GC constantemente a ser chamado). É assim necessário realizar um estudo de quais os possíveis pontos de optimização.

### ISSUE 04111:Haversine

## Recently Solved Issues

### ISSUE 07041:Scheduler
O problema foi "resolvido" através da especificação do valor da duração das tarefas das probes como sendo 0 (`"duration":0`). Embora isto funcione bem para a `AccelerometerSensorProbe` e para a `GravitySensorProbe`, a sampling rate da `LocationProbe` ainda deixa a desejar.

### ISSUE 04103:UnexpectedCrash
Por vezes, ao iniciar pela primeira vez uma sessão de sensing, a aplicação crasha de forma inesperada. Após a análise do BUG report, gerado pelo dispositivo, foram identificados várias excepções fatais causadas por uma NullPointerException. Esta é sempre causada em `LocationPipeline.java:188`, quando é chamado o método `JsonElement.getAsString` sobre um objecto não existente. Esta excepção ocorre quando o provider da localização é a rede e o método acede ao JsonElemente "extras", que por sua vez é não existente.

A solução para este problema passa assim por verificar se o campo "extras" existe ou não e apenas caso este exista são adicionados os campos especiais à nova amostra.

### ISSUE 07043:EmptyDB
Havia um bug no método `ScoutArchive.getDelegateArchive()`, em que caso o `delegateArchive` já existisse `null` era retornado.

### ISSUE 04104:LazyApp
O problema estava na forma como o consumo do estado e posterior actualização estavam a ser realizados. A TimerTask foi assim substituída por um `Runnable` invocado por um `Handler` (`handler.postDelayed(runnable, interval)`). 