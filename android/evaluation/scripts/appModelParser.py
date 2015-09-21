import sys
import json
from pprint import pprint

#Constants
version     = "@version"
identifier  = "@identifier"
time        = "@execution_time"


filename = sys.argv[1]
print("Parsing file '"+filename+"'.")



with open(filename) as data_file:
    data = json.load(data_file)
    model= data["@model"]
    for stage in model:
        print(stage[identifier]+" "+str(stage[time]))
