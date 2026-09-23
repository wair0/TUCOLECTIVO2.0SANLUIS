import os
from PIL import Image, ImageDraw, ImageFilter

def create_cyberpunk_icon(size):
    img = Image.new('RGBA', (size, size), (13, 14, 21, 255))
    s = size / 512.0
    
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    bus_rect = [int(120*s), int(140*s), int(392*s), int(400*s)]
    g_draw.rounded_rectangle(bus_rect, radius=int(40*s), outline=(0, 240, 255, 180), width=int(12*s))
    glow = glow.filter(ImageFilter.GaussianBlur(int(10*s)))
    img = Image.alpha_composite(img, glow)
    
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle(bus_rect, radius=int(40*s), outline=(0, 240, 255, 255), width=int(8*s))
    
    windshield = [int(150*s), int(180*s), int(362*s), int(270*s)]
    draw.rounded_rectangle(windshield, radius=int(15*s), outline=(0, 240, 255, 255), width=int(6*s), fill=(0, 240, 255, 45))
    
    dest = [int(180*s), int(152*s), int(332*s), int(170*s)]
    draw.rounded_rectangle(dest, radius=int(5*s), fill=(255, 0, 127, 255))
    
    draw.ellipse([int(155*s), int(325*s), int(195*s), int(365*s)], outline=(0, 240, 255, 255), width=int(5*s), fill=(0, 240, 255, 120))
    draw.ellipse([int(317*s), int(325*s), int(357*s), int(365*s)], outline=(0, 240, 255, 255), width=int(5*s), fill=(0, 240, 255, 120))
    
    grille = [int(220*s), int(332*s), int(292*s), int(358*s)]
    draw.rounded_rectangle(grille, radius=int(8*s), outline=(255, 0, 127, 255), width=int(4*s), fill=(255, 0, 127, 60))
    
    draw.rounded_rectangle([int(85*s), int(200*s), int(115*s), int(260*s)], radius=int(8*s), outline=(0, 240, 255, 255), width=int(5*s), fill=(13, 14, 21, 255))
    draw.line([(int(115*s), int(220*s)), (int(120*s), int(220*s))], fill=(0, 240, 255, 255), width=int(5*s))
    draw.line([(int(115*s), int(240*s)), (int(120*s), int(240*s))], fill=(0, 240, 255, 255), width=int(5*s))
    
    draw.rounded_rectangle([int(397*s), int(200*s), int(427*s), int(260*s)], radius=int(8*s), outline=(0, 240, 255, 255), width=int(5*s), fill=(13, 14, 21, 255))
    draw.line([(int(392*s), int(220*s)), (int(397*s), int(220*s))], fill=(0, 240, 255, 255), width=int(5*s))
    draw.line([(int(392*s), int(240*s)), (int(397*s), int(240*s))], fill=(0, 240, 255, 255), width=int(5*s))
    
    draw.line([(int(140*s), int(400*s)), (int(372*s), int(400*s))], fill=(255, 0, 127, 255), width=int(6*s))
    
    draw.rounded_rectangle([int(145*s), int(395*s), int(195*s), int(420*s)], radius=int(6*s), fill=(0, 240, 255, 255))
    draw.rounded_rectangle([int(317*s), int(395*s), int(367*s), int(420*s)], radius=int(6*s), fill=(0, 240, 255, 255))
    
    return img

def make_round(img):
    sz = img.size[0]
    mask = Image.new('L', (sz, sz), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, sz, sz), fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out

densities = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192
}

for folder, sz in densities.items():
    path = os.path.join('app/src/main/res', folder)
    os.makedirs(path, exist_ok=True)
    icon = create_cyberpunk_icon(sz)
    icon.save(os.path.join(path, 'ic_launcher.png'))
    make_round(icon).save(os.path.join(path, 'ic_launcher_round.png'))

print("Iconos generados exitosamente.")
