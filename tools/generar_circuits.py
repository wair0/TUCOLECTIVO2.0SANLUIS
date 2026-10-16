#!/usr/bin/env python3
"""Estilo CORPORATIVO: ya no hay circuitos. Este script solo escribe un circuits.css vacío para que index.html y el workflow 'Generar circuits.css' sigan funcionando."""
import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'assets', 'circuits.css')
with open(OUT, 'w', encoding='utf-8') as f:
    f.write('/* TU COLECTIVO 2.0 — estilo CORPORATIVO: no hay circuitos. Este archivo queda vacío a propósito (lo genera tools/generar_circuits.py). */\n')
