import sys
import json

import zmq


# e.g. tcp://jord.research.ets.org:5555
broker_addr = sys.argv[1]

context = zmq.Context()

print("Connecting to server...", file=sys.stderr)
socket = context.socket(zmq.REQ)
socket.connect(broker_addr)

request = json.dumps({"tokens" : [["Hello", ",", "world", "!"], ["How", "are", "you", "?"]]})
print("Sending request", request, file=sys.stderr)
socket.send_string(request, 0)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["tags"])

request = json.dumps(result)
print("Sending request", result, file=sys.stderr)
socket.send_string(request, 0)

reply = socket.recv_string()

print("Raw result:", reply)
result = json.loads(reply)
print(result["trees"])

socket.close()
context.term()
