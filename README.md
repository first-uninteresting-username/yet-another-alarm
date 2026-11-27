# yet-another-alarm

An Android alarm clock. To snooze, answer a trivia question from [Open Trivia Database](https://opentdb.com). A wrong answer means waiting for the next question.

- Multiple alarms with repeat days and a per-alarm snooze length
- Plays your own sound files; vibrates if the file is gone
- Material 3 Expressive, Material You colors
- No proprietary dependencies

Requires Android 12+.

## Install

[<img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="48">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.first_uninteresting_username.yetanotheralarm%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Ffirst-uninteresting-username%2Fyet-another-alarm%22%2C%22author%22%3A%22first-uninteresting-username%22%2C%22name%22%3A%22yet-another-alarm%22%7D)

Or add this repository's URL in [Obtainium](https://github.com/ImranR98/Obtainium), or download the APK from Releases.

On first launch, grant the permissions the app lists, or alarms may not ring.

## Build

```sh
devenv shell -- ./gradlew assembleDebug
```

## License

GPL-3.0. Trivia questions are from Open Trivia Database, licensed CC BY-SA 4.0.
