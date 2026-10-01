# FAQs / Troubleshooting guide
[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---


If you are trying to figure out why a specific test failed, start with [debugging-tests.md](debugging-tests.md).

### Setting Environment Variables:

You can set environment variables

From Mac OSX or Linux:

    export PLATFORM=android

From Windows:

    set PLATFORM=android

Example: 
On Linux / OSX OS:

    PLATFORM=android PARALLEL=2 ./gradlew run

On Windows OS:

    set PLATFORM=android
    set PARALLEL=2
    gradlew.bat run
