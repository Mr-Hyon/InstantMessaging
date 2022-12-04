# InstantMessaging

## Group Members:  
Yisheng Zhang, Xiaoxing Chen

## How to run this program

### Step 1: Generate RSA key pair for potential clients

<p>
Run the following three commands to generate public key and private key: </br>

'openssl genrsa -out private_key.pem 2048' </br>
'openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt' </br>
'openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der' </br>
</p>

### Step 2: create folder for clients

<p>
Under 'Client' folder, create a folder and name it with username (i.e. Alice), put 'private_key.der' and 'public_key.der' generated from Step 1 under this username folder.<br>
Under 'Server/Account' folder, create a folder and name it with username(i.e. Alice), put 'public_key.der' generated from Step 1 under this folder.
</p>

### Step 3: How to Compile

<p>
Under Client folder, execute the following command:<br>
javac *.java <br>

Under Server folder, execute the following command:<br>
javac *.java
</p>

### Step 4: How to Run

<p>
Under Server folder, execute the following command:<br>
java Server <br>

Under Client folder, execute the following command:<br>
java Client <br>

### Step 5: Basic Functionality
1. As a client, enter your username to login(must have created folder and set up rsa key properly)
2. type '/list' to see potential online user
3. type '/chat <target_username>' to start a chat session with another user. 
4. During a chat session, type '/end' to end the chat session. 
5. While not in a  chat session, type '/exit' to disconnect from server.

### Special Note
1. To launch mutilple clients, create a terminal for each client and execute Client program. <br>
2. You must launch Server program before Client program<br>
3. If it shows fail to generate a session key when you try to chat to someone (which sometimes could happen when a bad key is generated), try resend the request or simply restart the program to retry will usually fix the problem.

</p>