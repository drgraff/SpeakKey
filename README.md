# SpeakKey

SpeakKey is an Android application that enables voice-driven transcription, AI-powered response generation (via OpenAI), and optional automated typing to a connected device using InputStick.

---

## ✨ Features

- 🎙️ Record audio and transcribe using OpenAI Whisper API
- 🎵 Records microphone input as true MP3 using the LAME encoder, enabling audio uploads to ChatGPT
- Display that transcription in an editable text box in the app
- 🤖 Send transcribed text to ChatGPT (configurable model)
- Display that response in an editable text box in the app
- ⌨️ Type response to PC via InputStick USB HID emulation
- ⏱️ Visual timer + flashing indicator while recording
- 📨 Auto-send toggle after recording (configurable)
- ✅ Persistent app settings (API key, Whisper URL, model, language, etc.)
- 🔘 Toggle InputStick functionality on/off
- ⚙️ Settings UI with input fields for OpenAI and Whisper configuration
- Modern black and white minimalist design layout
- Whole app dark mode option
- Uses code from https://github.com/inputstick/InputStickAPI-Android/ to control InputStick peripheral connected by Bluetooth

---

## 📸 UI Overview

- **Start Recording** button — begins capturing audio
- **Pause Recording** button — pauses capture and offers the option to continue on, adding to the recording
- **Stop Recording** button — ends capture and optionally auto-sends
- **Send to Whisper** button — manually sends last recording to Whisper, shows response transcription in the Whisper Text Box
- **Clear Recording** — clear last recording and delete associated file
- **Clear Transcription** — clear last transcription and delete associated recording file
- **Send to ChatGPT** button — manually sends last transcript, shows response text in the ChatGPT Response Text Box
- **Clear ChatGPT Response** — clear last ChatGPT Response Text Box
- **Send to InputStick** button — manually sends text from ChatGPT response text box to InputStick
- **Settings in Navigation drawer** — configure API key, model, Whisper endpoint, language, InputStick toggle, dark mode
- **Checkbox** — enable/disable auto-send to Whisper after recording
- **Checkbox** — enable/disable auto-send to InputStick after getting ChatGPT response
- **Timer and Red Dot** — visible feedback during recording

---


## 🧪 Requirements

- OpenAI API key
- InputStick device + USB receiver (optional)

---



## 🛠️ Dependencies

- Android Gradle Plugin 8.2.2
- Kotlin Gradle Plugin 1.9.22
- Minimum Android SDK 24
- Target SDK 33
- Gradle 8.14


## 📤 Sending Flow

```plaintext
[Voice] → [Whisper API] → [Transcript] → [Show in editable Text box] → [ChatGPT API] → [Response] → [Show in editable Text box] → [InputStick]

                                                          
```


## 🤝 Acknowledgments

- [OpenAI API](https://platform.openai.com/)
- [InputStick](https://www.inputstick.com/)


 © 2025 David Graff

