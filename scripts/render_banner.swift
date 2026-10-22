import AppKit

// Renders the 320x180dp Leanback banner at xhdpi (640x360 px) using the STYLES.md tokens.
let out = CommandLine.arguments[1]
let w = 640.0, h = 360.0
let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: Int(w), pixelsHigh: Int(h), bitsPerSample: 8,
                           samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                           bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
let ctx = NSGraphicsContext.current!.cgContext

func c(_ hex: UInt32, _ a: CGFloat = 1) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 0xff) / 255, green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255, alpha: a)
}
let bg = c(0x0B0B0F), accent = c(0x0A84FF), primary = c(0xF5F5F7), secondary = c(0xA1A1AA)

bg.setFill(); ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))
// soft accent glow
let glow = NSGradient(starting: c(0x0A84FF, 0.35), ending: c(0x0A84FF, 0))!
glow.draw(fromCenter: NSPoint(x: 150, y: 180), radius: 0, toCenter: NSPoint(x: 150, y: 180), radius: 230, options: [])

// icon (same geometry as ic_aircast.xml, viewport 48 -> scale 3, centered at x=150)
ctx.saveGState()
ctx.translateBy(x: 78, y: 180 + 72)
ctx.scaleBy(x: 3, y: -3)
accent.setStroke(); accent.setFill()
let frame = NSBezierPath()
frame.move(to: NSPoint(x: 33, y: 34)); frame.line(to: NSPoint(x: 39, y: 34))
frame.appendArc(withCenter: NSPoint(x: 39, y: 30), radius: 4, startAngle: 90, endAngle: 0, clockwise: true)
frame.line(to: NSPoint(x: 43, y: 12))
frame.appendArc(withCenter: NSPoint(x: 39, y: 12), radius: 4, startAngle: 0, endAngle: -90, clockwise: true)
frame.line(to: NSPoint(x: 9, y: 8))
frame.appendArc(withCenter: NSPoint(x: 9, y: 12), radius: 4, startAngle: 270, endAngle: 180, clockwise: true)
frame.line(to: NSPoint(x: 5, y: 30))
frame.appendArc(withCenter: NSPoint(x: 9, y: 30), radius: 4, startAngle: 180, endAngle: 90, clockwise: true)
frame.line(to: NSPoint(x: 15, y: 34))
frame.lineWidth = 3; frame.lineJoinStyle = .round; frame.stroke()
let tri = NSBezierPath(); tri.move(to: NSPoint(x: 24, y: 28)); tri.line(to: NSPoint(x: 33, y: 40)); tri.line(to: NSPoint(x: 15, y: 40)); tri.close(); tri.fill()
for (r, lw) in [(8.0, 2.5), (13.0, 2.5)] {
    let arc = NSBezierPath()
    arc.appendArc(withCenter: NSPoint(x: 24, y: 21.5 + r * 0.75), radius: r, startAngle: 220, endAngle: 320, clockwise: false)
    arc.lineWidth = lw; arc.lineCapStyle = .round; arc.stroke()
}
ctx.restoreGState()

let title = NSAttributedString(string: "AirCast TV", attributes: [
    .font: NSFont.systemFont(ofSize: 64, weight: .semibold), .foregroundColor: primary])
title.draw(at: NSPoint(x: 268, y: 178))
let sub = NSAttributedString(string: "Receptor AirPlay", attributes: [
    .font: NSFont.systemFont(ofSize: 30, weight: .regular), .foregroundColor: secondary])
sub.draw(at: NSPoint(x: 270, y: 132))

NSGraphicsContext.restoreGraphicsState()
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: out))
