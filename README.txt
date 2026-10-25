Job Order Queue and Tracking System for MNL Kingpin - Quezon Branch
DATASTRC Final Project, C2B
Maandal, Macaraig, Mateo, Tengonciang

HOW TO RUN (Command Prompt / Terminal, inside this folder)
  1. Compile:   javac -d out src/*.java
  2. Run:       java -cp out Main
  3. Start over with the sample data:   java -cp out Main --reset

HOW TO RUN IN VS CODE
  Open this whole folder (not just src), open src/Main.java, click Run.
  The data folder must be next to src, so always open the main folder.

NEEDS: Java 11 or newer (JDK 17 recommended).

FOLDERS
  src/          all the Java files
  data/         the saved job orders (CSV files). The program saves after every change.
  sample-data/  a clean copy of the demo data, used by --reset
  reports/      reports you choose to save (created automatically)

DEMO DATE
  data/settings.txt has demoDate=2026-10-29 so the demo always shows the same
  results. Delete the date (leave "demoDate=") to use the real date.
