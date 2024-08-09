# Run commands using Terminal and docker

1) Open the project in terminal

Project Properties -> Open in Terminal

![image](https://github.com/user-attachments/assets/c681e510-f308-433d-8e8f-4afc2a02a8dd)


2) Select SSH connection

   ![image](https://github.com/user-attachments/assets/72d51b58-8e9e-4b0a-b673-d1a7d31430be)

Configure your SSH connection

3) Go back to Project Properties -> Frameworks -> Laravel

![image](https://github.com/user-attachments/assets/19aa9a5f-068b-4127-b242-c8017bcc91df)

-> Select Remote terminal from the list

![image](https://github.com/user-attachments/assets/03514dfd-dbfc-49e8-b2de-c703ee0f3ec2)


4) Configure your docker settings

-> check the Use docker
-> write  your php docker image ex: "php-docker-php-1"
-> add remote bash type ex: "sh"
-> include pre commands. 
 Ex: sometimes the php app is not in the root folder of the docker image. So you will need
 to add a `cd /var/www/my-app && ` script before.

 ![image](https://github.com/user-attachments/assets/a4cdff39-b84b-4ded-a1e8-06299918bc80)

5) Go to run commands

   -> right click on Project Properties -> Laravel -> Run command

   ![image](https://github.com/user-attachments/assets/a07ba3bb-7258-435a-b135-456143a2121c)

6) Click on Refresh commands

   ![image](https://github.com/user-attachments/assets/47a50a9d-1bfc-4491-b530-fe52d0e87450)

---

Now if you select a command it should execute by clicking on the **Run** button 

![image](https://github.com/user-attachments/assets/d4749649-5a43-4965-9437-05087d7ff9d5)

The output terminal color can be edited on Netbeans -> Options -> Miscellaneous -> Terminal tab

![image](https://github.com/user-attachments/assets/473a2c05-8bdc-463b-b08b-c40df658a5ee)
