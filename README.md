# iiwaTOFAS
```mermaid
---
config:
  theme: neo
  layout: elk
---
flowchart TD
 subgraph subGraph0["Client Side"]
        B["Sends Command String"]
        A["ROS2 Driver - Task Client"]
        C["Python Log Client"]
  end
 subgraph Server_Startup["Server_Startup"]
        E["Initializes ServerClass"]
        D["TestServer - KUKA App"]
        F{"ServerClass"}
        G["Task Listener - Port 30001"]
        H["Log Listener - Port 30002"]
  end
 subgraph Connection_Management["Connection_Management"]
        I["Logger - Singleton"]
        K["ClientHandler - Task"]
        L["ClientHandler - Log"]
  end
 subgraph Command_Processing_Flow["Command_Processing_Flow"]
        M["CommandParser"]
        N["JSON String"]
        O["parsedData/parsedCommand.json"]
        P@{ label: "Sends 'FREE/ID#' Response" }
  end
 subgraph Java_Server_Robot_Controller["Java_Server_Robot_Controller"]
        Server_Startup
        Connection_Management
        Command_Processing_Flow
  end
 subgraph Log_Client_Processing["Log_Client_Processing"]
        Q["Python Log Client"]
        R["Formats & Colors Output"]
        S["Terminal Display"]
  end
    A --> B
    D --> E
    E --> F
    F --> G & H
    C -- Connect --> H
    H -- Set Handler --> I
    A -- Connect (Requires Log Client) --> G
    G -- On Connect --> K
    H -- On Connect --> L
    G -- Sends 'FREE|0#' --> A
    B -- Command Received --> K
    K -- Calls parseCommand() --> M
    M -- Returns ParsedCommand Object --> K
    K -- Calls toJson() --> N
    K -- Logs JSON --> I
    I -- Sends JSON --> L
    K -- Saves JSON to File --> O
    K -- Gets ID from ParsedCommand --> P
    P --> A
    L -- Sends JSON --> C
    C -- Receives JSON --> Q
    Q -- Parses JSON --> R
    R --> S
    Connection_Management --> Java_Server_Robot_Controller
    P@{ shape: rect}
    style A fill:#e0f2f7,stroke:#333,stroke-width:2px
    style C fill:#e0f2f7,stroke:#333,stroke-width:2px
    style E fill:#f0f8ff,stroke:#333,stroke-width:1px
    style D fill:#f0f8ff,stroke:#333,stroke-width:1px
    style F fill:#f0f8ff,stroke:#333,stroke-width:1px
    style G fill:#f0f8ff,stroke:#333,stroke-width:1px
    style H fill:#f0f8ff,stroke:#333,stroke-width:1px
    style I fill:#fffacd,stroke:#333,stroke-width:1px
    style K fill:#fffacd,stroke:#333,stroke-width:1px
    style L fill:#fffacd,stroke:#333,stroke-width:1px
    style M fill:#fffacd,stroke:#333,stroke-width:1px
    style N fill:#e0ffe0,stroke:#333,stroke-width:1px
    style O fill:#ffe0e0,stroke:#333,stroke-width:1px
    style P fill:#e0f2f7,stroke:#333,stroke-width:2px
    style Q fill:#e0f2f7,stroke:#333,stroke-width:2px
    style R fill:#e0f2f7,stroke:#333,stroke-width:2px
    style S fill:#cceeff,stroke:#333,stroke-width:2px