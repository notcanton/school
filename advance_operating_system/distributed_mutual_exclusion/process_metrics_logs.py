import matplotlib.pyplot as plt

responseTimes = [0 for i in range(6)]
messages = [0 for i in range(6)]
count = [0 for i in range(6)]
inter_request_delay = [i*2 for i in range(6)]


for i in range(8):
    data_file = open("/home/013/k/kx/kxp210004/logs/metricsInterReq"+ str(i) + ".log")
    data_file = data_file.readlines()

    for line in data_file:
        values = line.split(" ")
        request_delay_idx= int(int(values[0])//2)

        if responseTimes[request_delay_idx]==0:
            responseTimes[request_delay_idx] += float(values[2].strip('\n'))
            messages[request_delay_idx] += int(values[1])
            count[request_delay_idx] += 1

messages_for_sd = [[[] for i in range(6)] for j in range(8)]
synchr_delay = [0 for i in range(6)]
csexecution_time = [0 for i in range(6)]

for i in range(8):
    data_file = open("/home/013/k/kx/kxp210004/logs/metricsInterReqSD"+ str(i) + ".log")
    data_file = data_file.readlines()

    while len(data_file)>0:
        #Change to 1 or 0 based on the type.
        curr_node = int(data_file.pop(0).split(" ")[1].strip('\n'))//2

        pre_condition = False
        if len(messages_for_sd[i][curr_node])>0:
            pre_condition = True

        while len(data_file)>0 and data_file[0][0]!="D":
            if pre_condition:
                continue
            messages_for_sd[i][curr_node].append(data_file.pop(0))
            

for i in range(6):
    all_messages = []

    for j in range(8):
        all_messages.extend(messages_for_sd[j][i])
    for k in range(len(all_messages)):
        all_messages[k] = all_messages[k].strip("\n").split(" ")

    all_messages = sorted(all_messages, key = lambda x: x[1])

    count_num_1 = 0
    for m in range(0, len(all_messages)-4, 2):
        csexecution_time[i] += int(all_messages[m+1][2]) - int(all_messages[m][2])
        count_num_1 += 1

    csexecution_time[i]/=count_num_1
    
    count_num_2 = 0
    for m in range(1, len(all_messages)-4, 2):
        if (int(all_messages[m+1][2]) - int(all_messages[m][2]))<0:
            continue
        synchr_delay[i] += int(all_messages[m+1][2]) - int(all_messages[m][2])
        count_num_2 += 1
    
    synchr_delay[i]/=count_num_2
print(csexecution_time, synchr_delay)
responseTimes = [responseTimes[i]/count[i] for i in range(len(responseTimes))]
messages = [messages[i]/count[i] for i in range(len(messages))]
throughput = [(1/((csexecution_time[i] + synchr_delay[i])/1000)) for i in range(len(csexecution_time))]

# plt.title('Response Time (with mean CS=5ms)')
plt.title('Response Time (with mean Inter Request Delay=5ms)')
plt.plot(inter_request_delay, responseTimes, 'o-')
plt.xlabel('CS Execution Time Mean (in ms)')
plt.ylabel('Response Time (in ms)')
plt.savefig('./responseTimeInterReqConstant.jpg')
plt.clf()

plt.title('Number of Messages (with mean Inter Request Delay=5ms)')
plt.plot(inter_request_delay, messages, 'o-')
# plt.xlabel('Inter Request Delay Time (in ms)')
plt.xlabel('CS Execution Time Mean (in ms)')
plt.ylabel('Number of Messages')
plt.savefig('./messagesInterReqConstant.jpg')
plt.clf()

plt.title('System Throughput (with mean Inter Request Delay=5ms)')
plt.plot(inter_request_delay, throughput, 'o-')
# plt.xlabel('Inter Request Delay Time (in ms)')
plt.xlabel('CS Execution Time Mean (in ms)')
plt.ylabel('Throughput (s^-1)')
plt.savefig('./messagesInterReqThroughput.jpg')
