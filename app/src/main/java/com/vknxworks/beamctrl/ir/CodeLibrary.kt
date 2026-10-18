package com.vknxworks.beamctrl.ir

import com.vknxworks.beamctrl.data.DeviceType
import com.vknxworks.beamctrl.data.KeyRole
import com.vknxworks.beamctrl.data.Remote
import com.vknxworks.beamctrl.data.RemoteButton

/** A ready-made remote for a brand, built from that brand's well-known codes. */
class BrandTemplate(
    val id: String,
    val brand: String,
    val type: DeviceType,
    val note: String,
    val keys: List<Pair<KeyRole, IrCode>>,
) {
    val title: String get() = "$brand ${type.label}"

    fun code(role: KeyRole): IrCode? = keys.firstOrNull { it.first == role }?.second

    fun toRemote(name: String = title): Remote = Remote(
        name = name,
        type = type,
        brand = brand,
        buttons = keys.map { (role, code) -> RemoteButton(label = role.label, role = role, code = code) },
    )
}

object CodeLibrary {

    private fun digits(code: (Int) -> IrCode): List<Pair<KeyRole, IrCode>> =
        (0..9).map { KeyRole.digit(it) to code(it) }

    private val samsung = BrandTemplate(
        id = "samsung-tv",
        brand = "Samsung",
        type = DeviceType.TV,
        note = "Most Samsung TVs since about 2005, including current models",
        keys = buildList<Pair<KeyRole, IrCode>> {
            fun k(role: KeyRole, cmd: Int) = add(role to IrCode.Samsung32(0x07, cmd))
            k(KeyRole.POWER, 0x02); k(KeyRole.INPUT, 0x01); k(KeyRole.MUTE, 0x0F)
            k(KeyRole.VOL_UP, 0x07); k(KeyRole.VOL_DOWN, 0x0B); k(KeyRole.CH_UP, 0x12); k(KeyRole.CH_DOWN, 0x10)
            k(KeyRole.UP, 0x60); k(KeyRole.DOWN, 0x61); k(KeyRole.LEFT, 0x65); k(KeyRole.RIGHT, 0x62); k(KeyRole.OK, 0x68)
            k(KeyRole.BACK, 0x58); k(KeyRole.HOME, 0x79); k(KeyRole.MENU, 0x1A); k(KeyRole.EXIT, 0x2D)
            k(KeyRole.INFO, 0x1F); k(KeyRole.GUIDE, 0x4F)
            k(KeyRole.PLAY, 0x47); k(KeyRole.PAUSE, 0x4A); k(KeyRole.STOP, 0x46); k(KeyRole.REWIND, 0x45); k(KeyRole.FAST_FORWARD, 0x48)
            val samsungDigits = intArrayOf(0x11, 0x04, 0x05, 0x06, 0x08, 0x09, 0x0A, 0x0C, 0x0D, 0x0E)
            addAll(digits { IrCode.Samsung32(0x07, samsungDigits[it]) })
        },
    )

    private val lg = BrandTemplate(
        id = "lg-tv",
        brand = "LG",
        type = DeviceType.TV,
        note = "LG TVs, including webOS models",
        keys = buildList<Pair<KeyRole, IrCode>> {
            fun k(role: KeyRole, cmd: Int) = add(role to IrCode.Nec(0x04, cmd))
            k(KeyRole.POWER, 0x08); k(KeyRole.INPUT, 0x0B); k(KeyRole.MUTE, 0x09)
            k(KeyRole.VOL_UP, 0x02); k(KeyRole.VOL_DOWN, 0x03); k(KeyRole.CH_UP, 0x00); k(KeyRole.CH_DOWN, 0x01)
            k(KeyRole.UP, 0x40); k(KeyRole.DOWN, 0x41); k(KeyRole.LEFT, 0x07); k(KeyRole.RIGHT, 0x06); k(KeyRole.OK, 0x44)
            k(KeyRole.BACK, 0x28); k(KeyRole.HOME, 0x7C); k(KeyRole.MENU, 0x43); k(KeyRole.EXIT, 0x5B)
            k(KeyRole.INFO, 0xAA); k(KeyRole.GUIDE, 0xAB)
            k(KeyRole.PLAY, 0xB0); k(KeyRole.PAUSE, 0xBA); k(KeyRole.STOP, 0xB1); k(KeyRole.REWIND, 0x8F); k(KeyRole.FAST_FORWARD, 0x8E)
            addAll(digits { IrCode.Nec(0x04, 0x10 + it) })
        },
    )

    private val sony = BrandTemplate(
        id = "sony-tv",
        brand = "Sony",
        type = DeviceType.TV,
        note = "Sony Bravia and older Sony TVs (12-bit SIRC)",
        keys = buildList<Pair<KeyRole, IrCode>> {
            fun k(role: KeyRole, cmd: Int) = add(role to IrCode.Sirc(12, 0x01, cmd))
            k(KeyRole.POWER, 21); k(KeyRole.INPUT, 37); k(KeyRole.MUTE, 20)
            k(KeyRole.VOL_UP, 18); k(KeyRole.VOL_DOWN, 19); k(KeyRole.CH_UP, 16); k(KeyRole.CH_DOWN, 17)
            k(KeyRole.UP, 116); k(KeyRole.DOWN, 117); k(KeyRole.LEFT, 52); k(KeyRole.RIGHT, 51); k(KeyRole.OK, 101)
            k(KeyRole.HOME, 96); k(KeyRole.INFO, 58)
            // Sony numbers: 1 to 9 are commands 0 to 8, and 0 is command 9.
            addAll(digits { IrCode.Sirc(12, 0x01, if (it == 0) 9 else it - 1) })
        },
    )

    private val philips = BrandTemplate(
        id = "philips-tv",
        brand = "Philips",
        type = DeviceType.TV,
        note = "Philips TVs using RC6",
        keys = buildList<Pair<KeyRole, IrCode>> {
            fun k(role: KeyRole, cmd: Int) = add(role to IrCode.Rc6(0x00, cmd))
            k(KeyRole.POWER, 0x0C); k(KeyRole.INPUT, 0x38); k(KeyRole.MUTE, 0x0D)
            k(KeyRole.VOL_UP, 0x10); k(KeyRole.VOL_DOWN, 0x11); k(KeyRole.CH_UP, 0x20); k(KeyRole.CH_DOWN, 0x21)
            k(KeyRole.UP, 0x58); k(KeyRole.DOWN, 0x59); k(KeyRole.LEFT, 0x5A); k(KeyRole.RIGHT, 0x5B); k(KeyRole.OK, 0x5C)
            k(KeyRole.BACK, 0x0A); k(KeyRole.HOME, 0x54); k(KeyRole.INFO, 0x0F)
            addAll(digits { IrCode.Rc6(0x00, it) })
        },
    )

    private val vizio = BrandTemplate(
        id = "vizio-tv",
        brand = "Vizio",
        type = DeviceType.TV,
        note = "Vizio TVs, including SmartCast models",
        keys = buildList<Pair<KeyRole, IrCode>> {
            fun k(role: KeyRole, cmd: Int) = add(role to IrCode.Nec(0x04, cmd))
            k(KeyRole.POWER, 0x08); k(KeyRole.INPUT, 0x2F); k(KeyRole.MUTE, 0x09)
            k(KeyRole.VOL_UP, 0x02); k(KeyRole.VOL_DOWN, 0x03); k(KeyRole.CH_UP, 0x00); k(KeyRole.CH_DOWN, 0x01)
            k(KeyRole.UP, 0x45); k(KeyRole.DOWN, 0x46); k(KeyRole.LEFT, 0x47); k(KeyRole.RIGHT, 0x48); k(KeyRole.OK, 0x44)
            k(KeyRole.BACK, 0x4A); k(KeyRole.HOME, 0x2D); k(KeyRole.MENU, 0x4F); k(KeyRole.EXIT, 0x49); k(KeyRole.INFO, 0x1B)
            k(KeyRole.PLAY, 0x33); k(KeyRole.PAUSE, 0x37); k(KeyRole.STOP, 0x30)
            addAll(digits { IrCode.Nec(0x04, 0x10 + it) })
        },
    )

    val brands: List<BrandTemplate> = listOf(samsung, lg, sony, philips, vizio)

    fun brand(id: String): BrandTemplate? = brands.firstOrNull { it.id == id }
}
