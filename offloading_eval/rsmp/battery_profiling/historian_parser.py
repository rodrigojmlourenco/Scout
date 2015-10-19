import sys
import os
import re

directory = sys.argv[1]

files = os.listdir(directory)

def getKey(item):
    return item[0]

def historianparser(historian):
    filename = "_p_"+historian[:-5]+".txt"
    fout = open(filename, 'w')
    values = []
    pattern = re.compile('\[\'battery_level\'*')
    endparttern = re.compile('\]\);.*')
    with open(historian, 'r') as f:
        fout.write(filename+"\n")
        seconds_pattern = re.compile('^[0-9]+s.*')
        for line in f:
            if endparttern.match(line):
                break
            if pattern.match(line):
                aux1 = (line.split(',')[1]).split('=')[1]
                aux2 = aux1.split('(')
                batt = aux2[0]
                time = aux2[1].split('-+')[0]
                if time.startswith('+'):
                    time = time[1:]
                if time.endswith('-0s)\''):
                    time = time[:-5]
                    time = str(0)
                elif seconds_pattern.match(time):
                    time = time[:-1]
                else:
                    aux3 = time.split('m')
                    t_min = int(aux3[0])
                    t_sec = int(aux3[1][:-1])
                    time = str((t_min*60)+t_sec)
                values.append([batt, time])
        values = sorted(values, key=getKey, reverse=True)
        batt_i = int(values[0][0])
        batt_f = 0
        for v in values:
            fout.write(v[0]+" : "+v[1]+"\n")
            if int(v[1]) <= 1820 :
                batt_f = int(v[0])
        fout.write(": "+str(batt_i - batt_f))



                #print(batt+" -- "+time+"s")




for f in files:
    if f.endswith('.html'):
        historianparser(f)
