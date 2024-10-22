# oneswap-core
OneSwap core back-end

### Project Introduction
**OneSwap** is a decentralized exchange (DEX) aggregator for cryptocurrency token transactions. It offers two primary features: **Swap** and **Limit Orders**, providing users with flexibility in trading. Currently, OneSwap supports both **Uniswap v2** and **Balancer v2**, with plans to integrate additional protocols in the future.

### Architecture
OneSwap consists of front-end, back-end and smart contracts.
![OneSwap-flow](https://github.com/user-attachments/assets/99d89185-2392-4931-aade-c41051ddc588)
- The front-end is built with React, interacts with the blockchain using Web3.js, is hosted on S3, and uses CloudFront to optimize loading speed.
- Worker Back-End is responsible for processing front-end user demands, calculating and returning the best price to the user. Built on EC2 and using AWS Load Balancers and Auto Scaling to ensure service stability.
- Core Back-End is responsible for monitoring the liquidity changes and transaction records, which are stored in RDS and ElastiCache respectively. Use CloudWatch to monitor system exceptions and Lambda to switch backup EC2.
- There are two smart contracts: Aggregator and Limit Order, which handle spot transactions and limit transactions, respectively. The Limit Order contract is monitored and triggered by the Core Back-End for execution.
![OneSwap-architecture](https://github.com/user-attachments/assets/1baa12d0-85f9-4763-8f47-7a09351468ac)

### **Local Deployment**

1. Make sure that the MySQL and Redis environments exist.
2. Pull the Core Back-End image from Docker Hub:
    
    ```
    docker pull kai410705/oneswap-core:latest
    ```
    
3. Create the application.properties:
    
    ```
    # server setting
    spring.application.name=oneswap-core
    server.port=8080
    
    # service option
    blockchain=Sepolia
    ONESWAP_FEE=0.2
    
    # wallet private key
    BOT_WALLET_PRIVATE_KEY=yourWalletKey
    
    # API
    INFRA_ETHEREUM_HTTP_URL=yourKeyUrl
    INFRA_SEPOLIA_HTTP_URL=yourKeyUrl
    INFRA_ETHEREUM_WEBSOCKET_URL=yourKeyUrl
    INFRA_SEPOLIA_WEBSOCKET_URL=yourKeyUrl
    ALCHEMY_ETHEREUM_REST_URL=yourKeyUrl
    ALCHEMY_ETHEREUM_WEBSOCKET_UR=yourKeyUrl
    
    # address
    ONESWAP_V1_AGGREGATOR_SEPOLIA_ADDRESS=0x635D90a6D17d228423385518Ce597300C4fE0260
    ONESWAP_V1_LIMITORDER_SEPOLIA_ADDRESS=0x08dfC836a3343618ffB412FFaDF3B882cB98852b
    
    # MySQL
    spring.datasource.url=jdbc:mysql://localhost:3306/oneswap
    spring.datasource.username=yourUser
    spring.datasource.password=yourPassword
    spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    
    # JPA
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.database=mysql
    
    # Redis
    spring.data.redis.port=6379
    spring.data.redis.host=localhost
    spring.data.redis.password=yourPassword
    spring.data.redis.timeout=1000
    redis.ssl.enable=false
    
    # Log setting
    logging.file.name=application.log
    logging.logback.rollingpolicy.max-history=0
    ```
    
4. Run a container:
    
    ```
    docker run --rm --name oneswap-core-container -p 8080:8080 -v /your/payh/application.properties:/app/application.properties -v /your/payh/application.log:/app/application.log kai410705/oneswap-core:latest
    ```
    
5. Setup the Front-End and Core Back-End server.
    - Front-End: https://github.com/Drinkaiii/oneswap-interface
    - Core Back-End: https://github.com/Drinkaiii/oneswap-worker
