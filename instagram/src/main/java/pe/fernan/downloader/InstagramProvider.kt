package pe.fernan.downloader

import pe.fernan.downloader.social.core.BaseDownloaderSocial
import pe.fernan.downloader.social.core.model.DataVideo

class InstagramProvider : BaseDownloaderSocial() {
    override val baseUrl: String get() = "https://www.instagram.com"
    override val regex: String get() = ".instagram"

    override fun get(url: String): DataVideo? {
        TODO("Not yet implemented")
    }

}