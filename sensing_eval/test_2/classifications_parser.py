import sys

#Constants
default_delimiter = " | "
human = "Human"
scout = "Scout"
time  = "Time"

filename = sys.argv[1]
print("Parsing file '"+filename+"'.")

classificationLines = tuple(open(filename))

human_classes   = []
scout_classes   = []
times_values    = []

for instance in classificationLines: #For-each line in the file
    split_instance = instance.split(default_delimiter,3)
    human_classes.append(split_instance[0])
    scout_classes.append(split_instance[1])
    times_values.append(split_instance[2].strip())

#Print Average Time
avg_time = 0;
for time in times_values:
    avg_time += float(time);

avg_time = float(avg_time/len(times_values)/1000000)
print("Average Classification Time: %.4f" % avg_time+"ms")


asphalt_good= [0,0]
asphalt_bad = [0,0]
cobblestone_good = [0,0]
cobblestone_bad  = [0,0]

tp = 0
fp = 0
fn = 0

#Calculate Precision and FN
for i in range(len(human_classes)):
    if human_classes[i] == 'AsphaltGood':
        if human_classes[i] == scout_classes[i]:
            asphalt_good[0] += 1
            tp += 1
        else:
            asphalt_good[1] += 1
            fn += 1
    elif human_classes[i] == 'AsphaltBad':
        if human_classes[i] == scout_classes[i]:
            asphalt_bad[0] += 1
            tp += 1
        else:
            asphalt_bad[1] += 1
            fn += 1
    elif human_classes[i] == 'CobblestoneGood':
        if human_classes[i] == scout_classes[i]:
            cobblestone_good[0] += 1
            tp += 1
        else:
            cobblestone_good[1] += 1
            fn += 1
    elif human_classes[i] == 'CobblestoneBad':
        if human_classes[i] == scout_classes[i]:
            cobblestone_bad[0] += 1
            tp += 1
        else:
            cobblestone_bad[1] += 1
            fn += 1

print("\n\nClass\t\t| TP\t| FN ")
print("AsphaltGood\t| "+str(asphalt_good[0])+"\t| "+str(asphalt_good[1]))
print("AsphaltBad\t| "+str(asphalt_bad[0])+"\t| "+str(asphalt_bad[1]))
print("CobblestoneGood\t| "+str(cobblestone_good[0])+"\t| "+str(cobblestone_good[1]))
print("CobblestoneBad\t| "+str(cobblestone_bad[0])+"\t| "+str(cobblestone_bad[1]))


asphalt_good= [0,0]
asphalt_bad = [0,0]
cobblestone_good = [0,0]
cobblestone_bad  = [0,0]

#Calculate Precision and FP
for i in range(len(human_classes)):
    if scout_classes[i] == 'AsphaltGood':
        if human_classes[i] == scout_classes[i]:
            asphalt_good[0] += 1
        else:
            asphalt_good[1] += 1
            fp += 1
    elif scout_classes[i] == 'AsphaltBad':
        if human_classes[i] == scout_classes[i]:
            asphalt_bad[0] += 1
        else:
            asphalt_bad[1] += 1
            fp += 1
    elif scout_classes[i] == 'CobblestoneGood':
        if human_classes[i] == scout_classes[i]:
            cobblestone_good[0] += 1
        else:
            cobblestone_good[1] += 1
            fp += 1
    elif scout_classes[i] == 'CobblestoneBad':
        if human_classes[i] == scout_classes[i]:
            cobblestone_bad[0] += 1
        else:
            cobblestone_bad[1] += 1
            fp += 1

print("\n\nClass\t\t| TP\t| FP")
print("AsphaltGood\t| "+str(asphalt_good[0])+"\t| "+str(asphalt_good[1]))
print("AsphaltBad\t| "+str(asphalt_bad[0])+"\t| "+str(asphalt_bad[1]))
print("CobblestoneGood\t| "+str(cobblestone_good[0])+"\t| "+str(cobblestone_good[1]))
print("CobblestoneBad\t| "+str(cobblestone_bad[0])+"\t| "+str(cobblestone_bad[1]))

numSamples = len(human_classes)
precision  = float(tp / numSamples)
fprate     = float(fp / numSamples)
fnrate     = float(fn / numSamples)

print("\n\nSamples | Precision\t| FP Rate\t| FN Rate")
print(str(numSamples)+" \t| %.3f" % precision+"\t\t| %.3f" % fprate+"\t\t| %.3f" % fnrate)
