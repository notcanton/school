# contain lists of enter and exit timestamps
class Node:
    def __init__(self):
        self.enterCS = []
        self.exitCS  = []




# open config file and get number of nodes and number of requests per node
configFile = open("config.txt", "r")
for line in configFile:
    if line[0] != "#":
        args        = line.split(' ')
        numNodes    = args[0]
        numRequests = args[3]
        break

# look through all the log files and get start/end time of each CS of
# each node
nodes = []
for i in range(int(numNodes)):
    node     = Node()
    nodeFile = open("logs/log{}.log".format(i), "r")
    for line in nodeFile:
        args = line.split(':')
        if args[0] == "S":
            node.enterCS.append(int(args[1]))
        else:
            node.exitCS.append(int(args[1]))
    nodes.append(node)


success = True
for n in range(int(numRequests)):
    for i in range(1, len(nodes)):
        for j in range(i):
            if (i!=j):
                for reqInd in range(int(numRequests)):
                    if (nodes[i].exitCS[n] >= nodes[j].enterCS[reqInd]) and (nodes[j].exitCS[reqInd] >= nodes[i].enterCS[n]):
                        print("ERROR: node {} and node {} are executing their CS at the same time!".format(i, j))
                        print("\tnode {} enter-exit timestamp: {} - {}".format(i, nodes[i].enterCS[n], nodes[i].exitCS[n]))
                        print("\tnode {} enter-exit timestamp: {} - {}".format(j, nodes[j].enterCS[reqInd], nodes[j].exitCS[reqInd]))
                        success = False

if success:
    print("No CS overlapping detected")
