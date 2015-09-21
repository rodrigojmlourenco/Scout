import sys

#Constants
default_delimiter = " || "

filename = sys.argv[1]
print("Parsing file '"+filename+"'.")

testLines = tuple(open(filename))
test_values_count = len(testLines);
test_values_start = 3;

header_elems = testLines[2].split(default_delimiter,500)
elems_count = len(header_elems)

#Trim header white-spaces
for index in range(elems_count):
    header_elems[index] = header_elems[index].strip()

#Step 1 - Load the values onto a list
elems_values = []
for index in range(elems_count):
    elems_values.append([])

for test_value in testLines[3:]: #For-each line in the file
    split_values = test_value.split(default_delimiter,500)
    for i in range(elems_count): #For-each element in the line
        elems_values[i].append(split_values[i].strip())

#Step 2 - Parse the values onto a String
parsed_output = ""
for index in range(elems_count):
    parsed_output += header_elems[index]+"\n"
    for j in range(len(elems_values[index])):
        parsed_output += elems_values[index][j]+"\n"
    parsed_output += "=================================\n"
    parsed_output += "=================================\n"

output = open("./parsed_"+filename, "w+")
output.write(parsed_output)
