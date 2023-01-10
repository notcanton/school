# Distributed Mutual Exclusion

Implementation of Lamport's Algorithm plus Ricart and Agrawala's Algorithm

## Running the program
- Run 'launcher.sh' to start multiple the program multiple times based on 'config.txt'
- 'checker.py' tests the logged scaler time stamps to ensure mutex is maintained
- 'process_metric_logs.py' generates performance metrics based on output such as response time and system throughput

## Logging Level

- Severe: Program errors and single proccess in CS error
- Warning: Unexpected messages in taskQueue
- Info: Node initialization info 
- Fine: Mutex entering and leaving info
- Finer: TaskHandler processing info
- Finest: ListenChannel info