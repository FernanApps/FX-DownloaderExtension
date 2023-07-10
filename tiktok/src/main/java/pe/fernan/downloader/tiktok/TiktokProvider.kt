package pe.fernan.downloader.tiktok
import org.jsoup.Jsoup
import pe.fernan.downloader.social.core.BaseDownloaderSocial
import pe.fernan.downloader.social.core.model.Author
import pe.fernan.downloader.social.core.model.DataVideo
import pe.fernan.downloader.social.core.model.Source
import pe.fernan.downloader.social.core.utils.NiceOkHttp
import pe.fernan.downloader.social.core.utils.USER_AGENT_VALUE
import pe.fernan.downloader.social.core.utils.document
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


var currentTT: String? = null

class TiktokProvider : BaseDownloaderSocial()  {
    override val baseUrl: String
        get() = TODO("Not yet implemented")

    override val regex: String get() = ".tiktok."


    private val apiUrl = "https://ssstik.io/es"
    private val regexTT = Regex("tt:'(\\w+)'")
    private val apiPostUrl = "https://ssstik.io/abc?url=dl"
    private val apiPostSlideUrl = "https://r.ssstik.top/index.sh"

    override fun get(url: String): DataVideo? {
        try {
            if (currentTT == null) {

                fun mainResponse(): String? {
                    return Jsoup.connect(apiUrl)
                        .referrer(apiUrl)
                        .userAgent(USER_AGENT_VALUE)
                        .get().toString()
                }

                val body = mainResponse()
                val tt = regexTT.find(body!!)!!.groupValues[1]
                currentTT = tt
            }

            currentTT ?: throw Exception("currentTT is Empty")

            val document = NiceOkHttp(apiPostUrl).baseResponse(
                data = "id=${url}&locale=es&tt=$currentTT",
                method = NiceOkHttp.Method.POST
            ) {
                addHeader("Accept", "*/*")
                addHeader("Accept-Language", "en-US,en;q=0.5")
                //addHeader("Accept-Encoding", "gzip, deflate, br")
                addHeader("HX-Current-URL", apiUrl)
                addHeader("Origin", apiUrl)
                addHeader("Connection", "keep-alive")
                addHeader("Referer", apiUrl)
                addHeader("Sec-Fetch-Dest", "empty")
                addHeader("Sec-Fetch-Mode", "cors")
                addHeader("Sec-Fetch-Site", "same-origin")
                addHeader("TE", "trailers")
            }.document




            val authorElement = document!!.select("div>img.result_author").first()

            val author = Author(
                name = authorElement?.attr("alt") ?: "",
                avatar = authorElement?.attr("src") ?: ""
            )

            val description = replaceTags(
                document.select("div>p.maintext").text()
            )

            val sources = mutableListOf<Source>()
            println("::: Base :::")
            println(document)

            require(true){
                "div>a[class^=pure] : isEmpty"
            }

            document.select("div>a[class^=pure]").forEach { a ->
                var sourceUrl = a.attr("href")
                val name = a.text()
                // Not Image For Now

                if (name.contains("this slide")) {
                    return@forEach
                }

                var type = when {
                    name.lowercase().contains("mp3") -> Source.Type.MP3
                    name.lowercase().contains("as video") -> Source.Type.SLIDE
                    else -> Source.Type.MP4
                }

                if (type == Source.Type.SLIDE) {

                    val postUrl = a.attr("hx-post")
                    // [name=slides_data]
                    val hxInclude = a.attr("hx-include")

                    // Search Hidden Input
                    val input = document.select("input$hxInclude").first()!!
                    val dataName = input.attr("name")
                    val dataValue = URLEncoder.encode(
                        input.attr("value"), "UTF-8"
                    )!!

                    val slideResponse = NiceOkHttp(postUrl).baseResponse(
                        data = "${dataName}=${dataValue}",
                        method = NiceOkHttp.Method.POST,
                        timeOut = 5L to TimeUnit.MINUTES
                    ) {

                        addHeader("HX-Request", "true")
                        addHeader("HX-Trigger", "slides_generate")
                        addHeader("HX-Target", "slides_generate")
                        addHeader("HX-Current-URL", apiUrl)
                        addHeader("Origin", apiUrl)
                        addHeader("Connection", "keep-alive")
                        addHeader("Referer", "$apiUrl/")
                        addHeader("Sec-Fetch-Dest", "empty")
                        addHeader("Sec-Fetch-Mode", "cors")
                        addHeader("Sec-Fetch-Site", "cross-site")
                        addHeader("TE", "trailers")
                    }

                    val headerRedirect = slideResponse?.headers?.get("hx-redirect")

                    sourceUrl = headerRedirect!!
                    type = Source.Type.MP4
                }


                sources.add(Source(sourceUrl, name, type))
            }

            val finalSource = sources.distinctBy { it.url }

            return DataVideo(socialUrl = url, author, description, finalSource)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun replaceTags(txt: String) = Regex("#(\\w+)").replace(txt, "")

}


