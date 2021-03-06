package com.kakao.ifkakao.studio.test.domain.emoticon

import com.kakao.ifkakao.studio.domain.emoticon.EmoticonInformation
import com.kakao.ifkakao.studio.domain.emoticon.EmoticonRepository
import com.kakao.ifkakao.studio.domain.emoticon.EmoticonService
import com.kakao.ifkakao.studio.test.Mock
import com.kakao.ifkakao.studio.test.SpringDataConfig
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.chunked
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.ZoneId

@ContextConfiguration(classes = [SpringDataConfig::class])
class EmoticonServiceSpec(
    private val emoticonRepository: EmoticonRepository
) : ExpectSpec() {
    private val emoticonService = EmoticonService(repository = emoticonRepository)

    init {
        context("이모티콘 생성을 할 때") {
            val account = Mock.account(identified = true)
            val information = information()
            val images = images()
            expect("계정과 이모티콘 정보, 이미지가 있으면 이모티콘이 생성된다.") {
                val emoticon = emoticonService.create(account, information, images)
                emoticon.id shouldBeGreaterThan 0
                emoticon.authorId shouldBe account.id
                emoticon.title shouldBe information.title
                emoticon.description shouldBe information.description
                emoticon.choco shouldBe information.choco
                emoticon.images shouldContainAll images
            }
        }

        context("이모티콘을 조회할때") {
            val saved = Arb.localDate(
                minDate = LocalDate.of(2020, 1, 1),
                maxDate = LocalDate.of(2020, 1, 10)
            ).flatMap {
                Mock.emoticonEntity(it, ZoneId.systemDefault()).chunked(1..10).single()
            }.let {
                emoticonRepository.saveAll(it.chunked(100..1000).single())
            }

            expect("생성 시작 시간의 범위로 조회하면 해당 구간에 생성된 이모티콘을 조회 할 수 있다.") {
                val from = LocalDate.of(2020, 1, 5).atStartOfDay().atZone(ZoneId.systemDefault())
                val to = LocalDate.of(2020, 1, 8).atStartOfDay().atZone(ZoneId.systemDefault())
                val target = with(saved) {
                    val fromInstant = from.toInstant()
                    val toInstant = to.toInstant()
                    filter {
                        fromInstant <= it.createdAt && toInstant >= it.createdAt
                    }.map { it.id }
                }

                val emoticons = emoticonService.getAllCreatedAt(from, to)

                emoticons shouldHaveSize target.count()
                emoticons.map { it.id } shouldContainAll target
            }
        }
    }

    private fun information() = EmoticonInformation(
        title = Arb.string(10..100).single(),
        description = Arb.string(100..300).single(),
        choco = Arb.int(100..500).single()
    )

    private fun images() = Arb.stringPattern("([a-zA-Z0-9]{1,10})/([a-zA-Z0-9]{1,10})\\.jpg")
        .chunked(1..10)
        .single()
}