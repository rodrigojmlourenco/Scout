import sys
import os
import historian

directory = sys.argv[1]

if directory == 'h' or directory == '-h' or directory == 'help':
    print 'This scripts converts all the files in the specified directoy into battery-historian html pages.'
    exit()


files = os.listdir(directory)


for profile in files:
    if profile.endswith('.txt'):
        os.system('python historian.py '+directory+'\\'+profile+' >> '+directory+'\\'+os.path.splitext(profile)[0]+'.html')
