package simulation.services

import java.awt.Color

data class Palette(val colors: List<Color>) {

    constructor(vararg colors: String) :
            this(colors.map { Color.decode("#$it") })

    fun colorByIndex(index: Int) = colors[index % colors.size]

    companion object {

        val earthyTones =
            Palette(
                "797D62",
                "9B9B7A",
                "BAA587",
                "D9AE94",
                "F1DCA7",
                "FFCB69",
                "E8AC65",
                "D08C60",
                "B58463",
                "997B66"

            )

        // https://coolors.co/palette/70d6ff-ff70a6-ff9770-ffd670-e9ff70
        val pastelRainbow =
            Palette(
                "70D6FF",
                "FF70A6",
                "FF9770",
                "FFF670",
                "70FFEA",
                "D6FF70",
                "E9FF70"
            )

        // https://coolors.co/palette/012a4a-013a63-01497c-014f86-2a6f97-2c7da0-468faf-61a5c2-89c2d9-a9d6e5
        val costalBlues =
            Palette(
                "012a4a",
                "013a63",
                "01497c",
                "014f86",
                "2a6f97",
                "2c7da0",
                "468faf",
                "61a5c2",
                "89c2d9",
                "a9d6e5"
            )

        // https://coolors.co/palette/03071e-370617-6a040f-9d0208-d00000-dc2f02-e85d04-f48c06-faa307-ffba08
        val fieryRedSunset =
            Palette(
                "03071e",
                "370617",
                "6a040f",
                "9d0208",
                "d00000",
                "dc2f02",
                "e85d04",
                "f48c06",
                "faa307",
                "ffba08"
            )

        // https://coolors.co/palette/fbf8cc-fde4cf-ffcfd2-f1c0e8-cfbaf0-a3c4f3-90dbf4-8eecf5-98f5e1-b9fbc0
        val softRainbow =
            Palette(
                "fbf8cc",
                "fde4cf",
                "ffcfd2",
                "f1c0e8",
                "cfbaf0",
                "a3c4f3",
                "90dbf4",
                "8eecf5",
                "98f5e1",
                "b9fbc0"
            )

        // https://coolors.co/palette/007f5f-2b9348-55a630-80b918-aacc00-bfd200-d4d700-dddf00-eeef20-ffff3f
        val springGreenHarmony =
            Palette(
                "007f5f",
                "2b9348",
                "55a630",
                "80b918",
                "aacc00",
                "bfd200",
                "d4d700",
                "dddf00",
                "eeef20",
                "ffff3f"
            )

        // https://coolors.co/palette/edf2fb-e2eafc-d7e3fc-ccdbfd-c1d3fe-b6ccfe-abc4ff
        val blueSerenity =
            Palette(
                "edf2fb",
                "e2eafc",
                "d7e3fc",
                "ccdbfd",
                "c1d3fe",
                "b6ccfe",
                "abc4ff"
            )

    }

}
