# Project Orpheus TTS v6.0

An advanced, acoustic-based text-to-speech synthesizer written in Java. This version focuses on massively improving phonetic accuracy and the naturalness of synthesized sounds.

*(A screenshot of the application UI)*

---

## 🚀 How to Run (Recommended Method)

This is the easiest way to use the application. The package includes everything you need, and you do **not** need to have Java installed on your computer.

1.  **Download the `dist` folder.** You can get the latest pre-built version from the official link below:
    *   **[Download Project Orpheus TTS from Google Drive](https://drive.google.com/file/d/1pB_qduUDkYgDdSPze0M6d952HYJKqDAV/view?usp=sharing)**

2.  Unzip the file if necessary, and open the `dist` folder.

3.  Double-click the **`run.bat`** file to start the application.

That's it! The program will launch, ready to use.

---

## ✨ Key Features (The Acoustic Accuracy Update)

-   **PRONUNCIATION OVERHAUL:** Fixed gross mispronunciations like "tea", "synthesizer", and "advanced" by massively expanding the phonetic dictionary.
-   **ACOUSTIC VOICING MODEL:** Fixed the "d sounds like t" issue by modeling the acoustics of human voicing, creating a realistic murmur for voiced consonants.
-   **REFINED FRICATIVE ENVELOPES:** Fixed unnatural hissing sounds by giving fricatives ('s', 'sh', 'f') a sharper, more controlled volume envelope.
-   **FASTER PHONEME TRANSITIONS:** Reduced audio blending time between phonemes to create crisper, clearer transitions.
-   **INCREASED DEFAULT LAYERS:** Default synthesis layers increased to 30 for maximum acoustic richness out-of-the-box.

---

## For Developers (Building from Source)

If you have downloaded the full source code and want to compile or modify the project, use the following scripts located in the project's root directory.

#### Prerequisites
-   A **Java Development Kit (JDK) version 9 or newer** must be installed.
-   The JDK's `bin` directory must be added to your system's `PATH` environment variable.

#### Compile and Run
-   To quickly compile and run the application for testing, double-click **`comprun.bat`**.
-   This script will automatically find your installed JDK, compile the source code, and launch the program.

#### Create a New Distribution
-   To package your changes into a new, self-contained `dist` folder:
    1. Open **`create_dist.bat`** in a text editor.
    2. Confirm the `JAVA_HOME` variable at the top points to your JDK installation.
    3. Save the file and double-click it to run.
-   A new `dist` folder will be generated, which you can then zip and share.

## 📁 Project Structure

ProjectOrpheus/
|—— src/
|   |—— Main/
|       |—— ProjectOrpheusTTS.java      # Main class, GUI setup
|       |—— SynthesizerEngine.java      # Core audio synthesis logic
|       |—— PhoneticTranscriber.java    # Text-to-phoneme conversion
|       |—— BiquadFilter.java           # Audio filter utility
|
|—— comprun.bat                         # For developers: Compiles and runs from source.
|—— create_dist.bat                     # For developers: Creates a new distributable package.
|
|—— dist/                               # For end-users: Contains the distributable application.
    |—— ProjectOrpheus.jar
    |—— jre/

    |—— run.bat





---

## 📜 Full Documentation

For a detailed, class-by-class technical breakdown of the engine's architecture, the full Javadoc is available.

*   **[Browse the Full API Documentation](./docs/index.html)**
