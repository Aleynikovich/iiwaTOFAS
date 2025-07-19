# iiwaTOFAS
graph TD
subgraph Client Side
A[ROS2 Driver (Task Client)] --> B(Sends Command String)
C[Python Log Client]
end

    subgraph Java Server (Robot Controller)
        subgraph Server Startup
            D[TestServer (KUKA App)] --> E(Initializes ServerClass)
            E --> F{ServerClass}
            F --> G[Task Listener (Port 30001)]
            F --> H[Log Listener (Port 30002)]
        end

        subgraph Connection Management
            C -- Connect --> H
            H -- Set Handler --> I[Logger (Singleton)]
            A -- Connect (Requires Log Client) --> G
            G -- On Connect --> K[ClientHandler (Task)]
            H -- On Connect --> L[ClientHandler (Log)]
            G -- Sends "FREE|0#" --> A
        end

        subgraph Command Processing Flow
            B -- Command Received --> K
            K -- Calls parseCommand() --> M[CommandParser]
            M -- Returns ParsedCommand Object --> K
            K -- Calls toJson() --> N[JSON String]
            K -- Logs JSON --> I
            I -- Sends JSON --> L
            K -- Saves JSON to File --> O[parsedData/parsedCommand.json]
            K -- Gets ID from ParsedCommand --> P(Sends "FREE/ID#" Response)
            P --> A
        end
    end

    subgraph Log Client Processing
        L -- Sends JSON --> C
        C -- Receives JSON --> Q[Python Log Client]
        Q -- Parses JSON --> R(Formats & Colors Output)
        R --> S[Terminal Display]
    end

    style A fill:#e0f2f7,stroke:#333,stroke-width:2px
    style C fill:#e0f2f7,stroke:#333,stroke-width:2px
    style D fill:#f0f8ff,stroke:#333,stroke-width:1px
    style E fill:#f0f8ff,stroke:#333,stroke-width:1px
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
