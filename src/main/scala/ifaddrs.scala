package nbwmon

import scala.scalanative._
import unsafe._
@extern
object Ifaddrs {
  import IfaddrsH._

  def getifaddrs(ifap: Ptr[Ptr[Ifaddrs]]): CInt = extern
  def freeifaddrs(ifa: Ptr[Ifaddrs]): Unit      = extern
}

object IfaddrsH {
  class SiocgifFlags(val value: CInt) extends AnyVal {
    def &(other: SiocgifFlags): Boolean =
      (value & other.value) == other.value
  }
  object SiocgifFlags {
    final val Up       = new SiocgifFlags(1 << 1)
    final val Loopback = new SiocgifFlags(1 << 4)
    final val Running  = new SiocgifFlags(1 << 6)
    // ...
  }

  // see scala-native#378

  // format: off
  type Ifaddrs = CStruct7[
    // scala-native#634 should be Ptr[Ifaddrs]
    Ptr[Byte],     // [1] ifa_next
    Ptr[CChar],    // [2] ifa_name
    CInt,          // [3] ifa_flags
    Ptr[SockAddr], // [4] ifa_addr
    Ptr[SockAddr], // [5] ifa_netmask
    Ptr[SockAddr], // [6] ifa_ifu
    Ptr[Byte]      // [7] ifa_data
  ]
  // format: on

  implicit class IfaddrsOps(val ptr: Ptr[Ifaddrs]) extends AnyVal {
    // scala-native#634
    // format: off
    def next: Ptr[Ifaddrs]        = (!(ptr.at1)).asInstanceOf[Ptr[Ifaddrs]]
    def name: Ptr[CChar]          = !(ptr.at2)
    def flags: SiocgifFlags       = new SiocgifFlags(!(ptr.at3))
    def addr: Option[SockAddrOps] = Option(new SockAddrOps(!(ptr.at4)))
    def data: Ptr[Byte]           = !(ptr.at7)
    // format: on

    // scala-native#367 we need to manually box Ptr[T]
    def iterator: Iterator[IfaddrsOps] = new Iterator[IfaddrsOps] {
      private var current = new IfaddrsOps(ptr)

      def hasNext: Boolean = {
        current.ptr.next != null
      }

      def next(): IfaddrsOps = {
        current = new IfaddrsOps(current.next)
        current
      }
    }
  }

  // format: off
  type RtnlLinkStats = CStruct22[
    CUnsignedInt, //  [1] rx_packets
    CUnsignedInt, //  [2] tx_packets
    CUnsignedInt, //  [3] rx_bytes
    CUnsignedInt, //  [4] tx_bytes
    CUnsignedInt, //  [5] rx_errors
    CUnsignedInt, //  [6] tx_errors
    CUnsignedInt, //  [7] rx_dropped
    CUnsignedInt, //  [8] tx_dropped
    CUnsignedInt, //  [9] multicast
    CUnsignedInt, // [10] collisions
    CUnsignedInt, // [11] rx_length_errors
    CUnsignedInt, // [12] rx_over_errors
    CUnsignedInt, // [13] rx_crc_errors
    CUnsignedInt, // [14] rx_frame_errors
    CUnsignedInt, // [15] rx_fifo_errors
    CUnsignedInt, // [16] rx_missed_errors
    CUnsignedInt, // [17] tx_aborted_errors
    CUnsignedInt, // [18] tx_carrier_errors
    CUnsignedInt, // [19] tx_fifo_errors
    CUnsignedInt, // [20] tx_heartbeat_errors
    CUnsignedInt, // [21] tx_window_errors
    CUnsignedInt  // [22] rx_compressed

    // we are limited to 22 fields scala-native#637
    // it's ok to ignore those since we don't allocate RtnlLinkStats64
    // CUnsignedInt, // [23] tx_compressed
    // CUnsignedInt  // [24] rx_nohandler
  ]
  // format: on

  implicit class RtnlLinkStatsOps(val ptr: Ptr[RtnlLinkStats]) extends AnyVal {
    def rxBytes: CUnsignedInt = !ptr.at3
    def txBytes: CUnsignedInt = !ptr.at4
  }

  class SaFamily(val value: CInt) extends AnyVal
  object SaFamily {
    final val Packet = new SaFamily(17)
    // ...
  }

  final val InterfaceNameSize = 14
  type _14 = Nat.Digit2[Nat._1, Nat._4]

  // format: off
  type SockAddr = CStruct2[
    CInt,              // [1] sa_family
    CArray[CChar, _14] // [2] sa_data
  ]
  // format: on

  implicit class SockAddrOps(val ptr: Ptr[SockAddr]) {
    def family: SaFamily         = new SaFamily(!(ptr.at1))
    def data: CArray[CChar, _14] = !(ptr.at2)
  }
}
