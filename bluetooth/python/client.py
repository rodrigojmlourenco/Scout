"""
A simple Python script to send messages to a sever over Bluetooth
using PyBluez (with Python 2).
"""

import bluetooth

serverMACAddress = '60:36:dd:94:ca:77'
port = 3
s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
s.connect((serverMACAddress, port))
while 1:
    text = raw_input() # Note change to the old (Python 2) raw_input
    if text == "quit":
        break
    s.send(text)
sock.close()
