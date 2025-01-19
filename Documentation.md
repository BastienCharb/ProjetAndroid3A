Bien sûr ! Voici une documentation détaillée expliquant le fonctionnement de l'application SingWithMe Karaoke, en mettant l'accent sur le parser et le téléchargement des fichiers MP3.

### Documentation de l'Application SingWithMe Karaoke

#### 1. Introduction

L'application SingWithMe Karaoke est une application Android qui permet aux utilisateurs de chanter en karaoké avec leurs chansons préférées. L'application affiche les paroles en temps réel et change la couleur des paroles au fur et à mesure que la chanson progresse, offrant une expérience de karaoké dynamique.

#### 2. Architecture de l'Application

L'application suit le modèle d'architecture Model-View-ViewModel (MVVM), qui sépare les composants de l'interface utilisateur (UI) de la logique métier. Cela rend le code plus modulaire et facile à tester.

- **Model**: Contient les structures de données et la logique métier.
- **View**: Contient les composants de l'interface utilisateur.
- **ViewModel**: Sert de pont entre le Model et la View, gérant les données liées à l'interface utilisateur de manière consciente du cycle de vie.

#### 3. Composants Clés

- **MainActivity**: Le point d'entrée principal de l'application.
- **MainViewModel**: Gère la logique métier et la liaison de données.
- **KaraokeScreen**: La fonction composable qui affiche l'interface de karaoké.
- **LyricLine**: Classe de données représentant une ligne de paroles avec un code temporel.
- **Song**: Classe de données représentant une chanson avec des métadonnées.

#### 4. Fonctionnalités Principales

- **Affichage des Paroles en Temps Réel**: Les paroles sont affichées en temps réel au fur et à mesure que la chanson progresse.
- **Changement de Couleur Dynamique**: Les paroles changent de couleur du blanc au rouge au fur et à mesure que le timestamp actuel progresse.
- **Sélection de Chanson**: Les utilisateurs peuvent sélectionner des chansons à partir d'une liste.
- **Contrôles Média**: Contrôles de lecture, pause et réinitialisation pour le lecteur multimédia.

#### 5. Parser de Markdown

Le parser de Markdown est utilisé pour analyser les fichiers Markdown contenant les paroles des chansons. Le fichier Markdown est téléchargé à partir d'une URL spécifiée et analysé pour extraire les lignes de paroles avec leurs codes temporels.

##### Fonction de Téléchargement et d'Analyse

```kotlin
private fun downloadAndParseMarkdown(url: String) {
    viewModelScope.launch {
        try {
            val markdownContent = withContext(Dispatchers.IO) {
                downloadFile(url)
            }
            if (markdownContent != null) {
                val parsedLyrics = parseMarkdown(markdownContent)
                _lyricLines.value = parsedLyrics
                logParsedLyrics(parsedLyrics)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Failed to download markdown file: ${e.message}"
        }
    }
}
```


##### Fonction de Téléchargement

```kotlin
private fun downloadFile(url: String): String? {
    var connection: HttpURLConnection? = null
    return try {
        connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        Log.d("MainViewModel", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val content = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                content.append(line)
                content.append("\n")
            }
            reader.close()
            content.toString()
        } else {
            Log.e("MainViewModel", "Failed to download file. Response code: $responseCode")
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("MainViewModel", "Exception occurred while downloading file: ${e.message}")
        null
    } finally {
        connection?.disconnect()
    }
}
```

##### Fonction de Parsing

```kotlin
fun parseMarkdown(content: String): List<LyricLine> {
    val lines = content.split("\n")
    val lyricLines = mutableListOf<LyricLine>()
    for (line in lines) {
        val parts = line.split("|")
        if (parts.size == 2) {
            val timecode = parts[0].trim()
            val lyric = parts[1].trim()
            lyricLines.add(LyricLine(timecode, lyric))
        }
    }
    return lyricLines
}
```

#### 6. Téléchargement des Fichiers MP3

Les fichiers MP3 sont téléchargés à partir d'une URL spécifiée et stockés dans le stockage externe de l'appareil. Le fichier MP3 est ensuite utilisé par le lecteur multimédia pour la lecture de la chanson.

##### Fonction de Téléchargement de MP3

```kotlin
private fun downloadMp3File(url: String, context: Context) {
    viewModelScope.launch {
        try {
            val mp3FileUri = withContext(Dispatchers.IO) {
                downloadFileToStorage(url, context)
            }
            _mp3FileUri.value = mp3FileUri
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Failed to download MP3 file: ${e.message}"
        }
    }
}
```

##### Fonction de Téléchargement dans le Stockage

```kotlin
private fun downloadFileToStorage(url: String, context: Context): Uri? {
    var connection: HttpURLConnection? = null
    return try {
        connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        Log.d("MainViewModel", "Response Code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "creep_${System.currentTimeMillis()}.mp3")
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SingWithMe")
            }

            val contentUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (contentUri == null) {
                Log.e("MainViewModel", "Failed to insert file into media store.")
                return null
            }

            val outputStream = contentResolver.openOutputStream(contentUri)

            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream?.write(buffer, 0, bytesRead)
            }

            outputStream?.flush()
            outputStream?.close()
            inputStream.close()

            MediaScannerConnection.scanFile(context, arrayOf(contentUri.toString()), null, null)

            contentUri
        } else {
            Log.e("MainViewModel", "Failed to download file. Response code: $responseCode")
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("MainViewModel", "Exception occurred while downloading file: ${e.message}")
        null
    } finally {
        connection?.disconnect()
    }
}
```

#### 7. Conclusion

L'application SingWithMe Karaoke offre une expérience de karaoké dynamique en affichant les paroles en temps réel et en changeant leur couleur au fur et à mesure que la chanson progresse. Le parser de Markdown et le téléchargement des fichiers MP3 sont des composants clés qui permettent à l'application de fonctionner de manière fluide et efficace.

#### 8. Références

- [Documentation officielle de Kotlin](https://kotlinlang.org/docs/home.html)
- [Documentation officielle d'Android](https://developer.android.com/docs)
- [Documentation officielle de Jetpack Compose](https://developer.android.com/jetpack/compose)
