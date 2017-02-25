package com.simplecity.amp_library.utils;

public class ColorPalette {

    public static int[] getPrimaryColors() {
        return ShuttleUtils.isUpgraded() ? PRIMARY_COLORS : PRIMARY_COLORS_FREE;
    }

    public static int[][] getPrimaryColorsSub() {
        return ShuttleUtils.isUpgraded() ? PRIMARY_COLORS_SUB : PRIMARY_COLORS_SUB_FREE;
    }

    public static int[] getAccentColors() {
        return ShuttleUtils.isUpgraded() ? ACCENT_COLORS : ACCENT_COLORS_FREE;
    }

    public static int[][] getAccentColorsSub() {
        return ShuttleUtils.isUpgraded() ? ACCENT_COLORS_SUB : ACCENT_COLORS_SUB_FREE;
    }

    private final static int[] PRIMARY_COLORS = new int[]{
            0xFFF44336,
            0xFFE91E63,
            0xFF9C27B0,
            0xFF673AB7,
            0xFF3F51B5,
            0xFF2196F3,
            0xFF03A9F4,
            0xFF00BCD4,
            0xFF009688,
            0xFF4CAF50,
            0xFF8BC34A,
            0xFFCDDC39,
            0xFFFFEB3B,
            0xFFFFC107,
            0xFFFF9800,
            0xFFFF5722,
            0xFF795548,
            0xFF9E9E9E,
            0xFF607D8B
    };

    private final static int[][] PRIMARY_COLORS_SUB = new int[][]{
            new int[]{
                    0xFFFFEBEE,
                    0xFFFFCDD2,
                    0xFFEF9A9A,
                    0xFFE57373,
                    0xFFEF5350,
                    0xFFF44336,
                    0xFFE53935,
                    0xFFD32F2F,
                    0xFFC62828,
                    0xFFB71C1C
            },
            new int[]{
                    0xFFFCE4EC,
                    0xFFF8BBD0,
                    0xFFF48FB1,
                    0xFFF06292,
                    0xFFEC407A,
                    0xFFE91E63,
                    0xFFD81B60,
                    0xFFC2185B,
                    0xFFAD1457,
                    0xFF880E4F
            },
            new int[]{
                    0xFFF3E5F5,
                    0xFFE1BEE7,
                    0xFFCE93D8,
                    0xFFBA68C8,
                    0xFFAB47BC,
                    0xFF9C27B0,
                    0xFF8E24AA,
                    0xFF7B1FA2,
                    0xFF6A1B9A,
                    0xFF4A148C
            },
            new int[]{
                    0xFFEDE7F6,
                    0xFFD1C4E9,
                    0xFFB39DDB,
                    0xFF9575CD,
                    0xFF7E57C2,
                    0xFF673AB7,
                    0xFF5E35B1,
                    0xFF512DA8,
                    0xFF4527A0,
                    0xFF311B92
            },
            new int[]{
                    0xFFE8EAF6,
                    0xFFC5CAE9,
                    0xFF9FA8DA,
                    0xFF7986CB,
                    0xFF5C6BC0,
                    0xFF3F51B5,
                    0xFF3949AB,
                    0xFF303F9F,
                    0xFF283593,
                    0xFF1A237E
            },
            new int[]{
                    0xFFE3F2FD,
                    0xFFBBDEFB,
                    0xFF90CAF9,
                    0xFF64B5F6,
                    0xFF42A5F5,
                    0xFF2196F3,
                    0xFF1E88E5,
                    0xFF1976D2,
                    0xFF1565C0,
                    0xFF0D47A1
            },
            new int[]{
                    0xFFE1F5FE,
                    0xFFB3E5FC,
                    0xFF81D4FA,
                    0xFF4FC3F7,
                    0xFF29B6F6,
                    0xFF03A9F4,
                    0xFF039BE5,
                    0xFF0288D1,
                    0xFF0277BD,
                    0xFF01579B
            },
            new int[]{
                    0xFFE0F7FA,
                    0xFFB2EBF2,
                    0xFF80DEEA,
                    0xFF4DD0E1,
                    0xFF26C6DA,
                    0xFF00BCD4,
                    0xFF00ACC1,
                    0xFF0097A7,
                    0xFF00838F,
                    0xFF006064
            },
            new int[]{
                    0xFFE0F2F1,
                    0xFFB2DFDB,
                    0xFF80CBC4,
                    0xFF4DB6AC,
                    0xFF26A69A,
                    0xFF009688,
                    0xFF00897B,
                    0xFF00796B,
                    0xFF00695C,
                    0xFF004D40
            },
            new int[]{
                    0xFFE8F5E9,
                    0xFFC8E6C9,
                    0xFFA5D6A7,
                    0xFF81C784,
                    0xFF66BB6A,
                    0xFF4CAF50,
                    0xFF43A047,
                    0xFF388E3C,
                    0xFF2E7D32,
                    0xFF1B5E20
            },
            new int[]{
                    0xFFF1F8E9,
                    0xFFDCEDC8,
                    0xFFC5E1A5,
                    0xFFAED581,
                    0xFF9CCC65,
                    0xFF8BC34A,
                    0xFF7CB342,
                    0xFF689F38,
                    0xFF558B2F,
                    0xFF33691E
            },
            new int[]{
                    0xFFF9FBE7,
                    0xFFF0F4C3,
                    0xFFE6EE9C,
                    0xFFDCE775,
                    0xFFD4E157,
                    0xFFCDDC39,
                    0xFFC0CA33,
                    0xFFAFB42B,
                    0xFF9E9D24,
                    0xFF827717
            },
            new int[]{
                    0xFFFFFDE7,
                    0xFFFFF9C4,
                    0xFFFFF59D,
                    0xFFFFF176,
                    0xFFFFEE58,
                    0xFFFFEB3B,
                    0xFFFDD835,
                    0xFFFBC02D,
                    0xFFF9A825,
                    0xFFF57F17
            },
            new int[]{
                    0xFFFFF8E1,
                    0xFFFFECB3,
                    0xFFFFE082,
                    0xFFFFD54F,
                    0xFFFFCA28,
                    0xFFFFC107,
                    0xFFFFB300,
                    0xFFFFA000,
                    0xFFFF8F00,
                    0xFFFF6F00
            },
            new int[]{
                    0xFFFFF3E0,
                    0xFFFFE0B2,
                    0xFFFFCC80,
                    0xFFFFB74D,
                    0xFFFFA726,
                    0xFFFF9800,
                    0xFFFB8C00,
                    0xFFF57C00,
                    0xFFEF6C00,
                    0xFFE65100
            },
            new int[]{
                    0xFFFBE9E7,
                    0xFFFFCCBC,
                    0xFFFFAB91,
                    0xFFFF8A65,
                    0xFFFF7043,
                    0xFFFF5722,
                    0xFFF4511E,
                    0xFFE64A19,
                    0xFFD84315,
                    0xFFBF360C
            },
            new int[]{
                    0xFFEFEBE9,
                    0xFFD7CCC8,
                    0xFFBCAAA4,
                    0xFFA1887F,
                    0xFF8D6E63,
                    0xFF795548,
                    0xFF6D4C41,
                    0xFF5D4037,
                    0xFF4E342E,
                    0xFF3E2723
            },
            new int[]{
                    0xFFFAFAFA,
                    0xFFF5F5F5,
                    0xFFEEEEEE,
                    0xFFE0E0E0,
                    0xFFBDBDBD,
                    0xFF9E9E9E,
                    0xFF757575,
                    0xFF616161,
                    0xFF424242,
                    0xFF212121
            },
            new int[]{
                    0xFFECEFF1,
                    0xFFCFD8DC,
                    0xFFB0BEC5,
                    0xFF90A4AE,
                    0xFF78909C,
                    0xFF607D8B,
                    0xFF546E7A,
                    0xFF455A64,
                    0xFF37474F,
                    0xFF263238
            }
    };

    private final static int[] PRIMARY_COLORS_FREE = new int[]{
            0xFFF44336,
//            0xFFE91E63,
            0xFF9C27B0,
//            0xFF673AB7,
//            0xFF3F51B5,
//            0xFF2196F3,
            0xFF03A9F4,
//            0xFF00BCD4,
            0xFF009688,
            0xFF4CAF50,
//            0xFF8BC34A,
//            0xFFCDDC39,
//            0xFFFFEB3B,
            0xFFFFC107,
//            0xFFFF9800,
//            0xFFFF5722,
//            0xFF795548,
            0xFF9E9E9E,
//            0xFF607D8B
    };

    private final static int[][] PRIMARY_COLORS_SUB_FREE = new int[][]{
            new int[]{
                    0xFFFFEBEE,
                    0xFFFFCDD2,
                    0xFFEF9A9A,
                    0xFFE57373,
                    0xFFEF5350,
                    0xFFF44336,
                    0xFFE53935,
                    0xFFD32F2F,
                    0xFFC62828,
                    0xFFB71C1C
            },
//            new int[]{
//                    0xFFFCE4EC,
//                    0xFFF8BBD0,
//                    0xFFF48FB1,
//                    0xFFF06292,
//                    0xFFEC407A,
//                    0xFFE91E63,
//                    0xFFD81B60,
//                    0xFFC2185B,
//                    0xFFAD1457,
//                    0xFF880E4F
//            },
            new int[]{
                    0xFFF3E5F5,
                    0xFFE1BEE7,
                    0xFFCE93D8,
                    0xFFBA68C8,
                    0xFFAB47BC,
                    0xFF9C27B0,
                    0xFF8E24AA,
                    0xFF7B1FA2,
                    0xFF6A1B9A,
                    0xFF4A148C
            },
//            new int[]{
//                    0xFFEDE7F6,
//                    0xFFD1C4E9,
//                    0xFFB39DDB,
//                    0xFF9575CD,
//                    0xFF7E57C2,
//                    0xFF673AB7,
//                    0xFF5E35B1,
//                    0xFF512DA8,
//                    0xFF4527A0,
//                    0xFF311B92
//            },
//            new int[]{
//                    0xFFE8EAF6,
//                    0xFFC5CAE9,
//                    0xFF9FA8DA,
//                    0xFF7986CB,
//                    0xFF5C6BC0,
//                    0xFF3F51B5,
//                    0xFF3949AB,
//                    0xFF303F9F,
//                    0xFF283593,
//                    0xFF1A237E
//            },
//            new int[]{
//                    0xFFE3F2FD,
//                    0xFFBBDEFB,
//                    0xFF90CAF9,
//                    0xFF64B5F6,
//                    0xFF42A5F5,
//                    0xFF2196F3,
//                    0xFF1E88E5,
//                    0xFF1976D2,
//                    0xFF1565C0,
//                    0xFF0D47A1
//            },
            new int[]{
                    0xFFE1F5FE,
                    0xFFB3E5FC,
                    0xFF81D4FA,
                    0xFF4FC3F7,
                    0xFF29B6F6,
                    0xFF03A9F4,
                    0xFF039BE5,
                    0xFF0288D1,
                    0xFF0277BD,
                    0xFF01579B
            },
//            new int[]{
//                    0xFFE0F7FA,
//                    0xFFB2EBF2,
//                    0xFF80DEEA,
//                    0xFF4DD0E1,
//                    0xFF26C6DA,
//                    0xFF00BCD4,
//                    0xFF00ACC1,
//                    0xFF0097A7,
//                    0xFF00838F,
//                    0xFF006064
//            },
            new int[]{
                    0xFFE0F2F1,
                    0xFFB2DFDB,
                    0xFF80CBC4,
                    0xFF4DB6AC,
                    0xFF26A69A,
                    0xFF009688,
                    0xFF00897B,
                    0xFF00796B,
                    0xFF00695C,
                    0xFF004D40
            },
            new int[]{
                    0xFFE8F5E9,
                    0xFFC8E6C9,
                    0xFFA5D6A7,
                    0xFF81C784,
                    0xFF66BB6A,
                    0xFF4CAF50,
                    0xFF43A047,
                    0xFF388E3C,
                    0xFF2E7D32,
                    0xFF1B5E20
            },
//            new int[]{
//                    0xFFF1F8E9,
//                    0xFFDCEDC8,
//                    0xFFC5E1A5,
//                    0xFFAED581,
//                    0xFF9CCC65,
//                    0xFF8BC34A,
//                    0xFF7CB342,
//                    0xFF689F38,
//                    0xFF558B2F,
//                    0xFF33691E
//            },
//            new int[]{
//                    0xFFF9FBE7,
//                    0xFFF0F4C3,
//                    0xFFE6EE9C,
//                    0xFFDCE775,
//                    0xFFD4E157,
//                    0xFFCDDC39,
//                    0xFFC0CA33,
//                    0xFFAFB42B,
//                    0xFF9E9D24,
//                    0xFF827717
//            },
//            new int[]{
//                    0xFFFFFDE7,
//                    0xFFFFF9C4,
//                    0xFFFFF59D,
//                    0xFFFFF176,
//                    0xFFFFEE58,
//                    0xFFFFEB3B,
//                    0xFFFDD835,
//                    0xFFFBC02D,
//                    0xFFF9A825,
//                    0xFFF57F17
//            },
            new int[]{
                    0xFFFFF8E1,
                    0xFFFFECB3,
                    0xFFFFE082,
                    0xFFFFD54F,
                    0xFFFFCA28,
                    0xFFFFC107,
                    0xFFFFB300,
                    0xFFFFA000,
                    0xFFFF8F00,
                    0xFFFF6F00
            },
//            new int[]{
//                    0xFFFFF3E0,
//                    0xFFFFE0B2,
//                    0xFFFFCC80,
//                    0xFFFFB74D,
//                    0xFFFFA726,
//                    0xFFFF9800,
//                    0xFFFB8C00,
//                    0xFFF57C00,
//                    0xFFEF6C00,
//                    0xFFE65100
//            },
//            new int[]{
//                    0xFFFBE9E7,
//                    0xFFFFCCBC,
//                    0xFFFFAB91,
//                    0xFFFF8A65,
//                    0xFFFF7043,
//                    0xFFFF5722,
//                    0xFFF4511E,
//                    0xFFE64A19,
//                    0xFFD84315,
//                    0xFFBF360C
//            },
//            new int[]{
//                    0xFFEFEBE9,
//                    0xFFD7CCC8,
//                    0xFFBCAAA4,
//                    0xFFA1887F,
//                    0xFF8D6E63,
//                    0xFF795548,
//                    0xFF6D4C41,
//                    0xFF5D4037,
//                    0xFF4E342E,
//                    0xFF3E2723
//            },
            new int[]{
                    0xFFFAFAFA,
                    0xFFF5F5F5,
                    0xFFEEEEEE,
                    0xFFE0E0E0,
                    0xFFBDBDBD,
                    0xFF9E9E9E,
                    0xFF757575,
                    0xFF616161,
                    0xFF424242,
                    0xFF212121
            },
//            new int[]{
//                    0xFFECEFF1,
//                    0xFFCFD8DC,
//                    0xFFB0BEC5,
//                    0xFF90A4AE,
//                    0xFF78909C,
//                    0xFF607D8B,
//                    0xFF546E7A,
//                    0xFF455A64,
//                    0xFF37474F,
//                    0xFF263238
//            }
    };

    private final static int[] ACCENT_COLORS = new int[]{
            0xFFFF1744,
            0xFFF50057,
            0xFFD500F9,
            0xFF651FFF,
            0xFF3D5AFE,
            0xFF2979FF,
            0xFF00B0FF,
            0xFF00E5FF,
            0xFF1DE9B6,
            0xFF00E676,
            0xFF76FF03,
            0xFFC6FF00,
            0xFFFFEA00,
            0xFFFFC400,
            0xFFFF9100,
            0xFFFF3D00
    };

    private final static int[][] ACCENT_COLORS_SUB = new int[][]{
            new int[]{
                    0xFFFF8A80,
                    0xFFFF5252,
                    0xFFFF1744,
                    0xFFD50000
            },
            new int[]{
                    0xFFFF80AB,
                    0xFFFF4081,
                    0xFFF50057,
                    0xFFC51162
            },
            new int[]{
                    0xFFEA80FC,
                    0xFFE040FB,
                    0xFFD500F9,
                    0xFFAA00FF
            },
            new int[]{
                    0xFFB388FF,
                    0xFF7C4DFF,
                    0xFF651FFF,
                    0xFF6200EA
            },
            new int[]{
                    0xFF8C9EFF,
                    0xFF536DFE,
                    0xFF3D5AFE,
                    0xFF304FFE
            },
            new int[]{
                    0xFF82B1FF,
                    0xFF448AFF,
                    0xFF2979FF,
                    0xFF2962FF
            },
            new int[]{
                    0xFF80D8FF,
                    0xFF40C4FF,
                    0xFF00B0FF,
                    0xFF0091EA
            },
            new int[]{
                    0xFF84FFFF,
                    0xFF18FFFF,
                    0xFF00E5FF,
                    0xFF00B8D4
            },
            new int[]{
                    0xFFA7FFEB,
                    0xFF64FFDA,
                    0xFF1DE9B6,
                    0xFF00BFA5
            },
            new int[]{
                    0xFFB9F6CA,
                    0xFF69F0AE,
                    0xFF00E676,
                    0xFF00C853
            },
            new int[]{
                    0xFFCCFF90,
                    0xFFB2FF59,
                    0xFF76FF03,
                    0xFF64DD17
            },
            new int[]{
                    0xFFF4FF81,
                    0xFFEEFF41,
                    0xFFC6FF00,
                    0xFFAEEA00
            },
            new int[]{
                    0xFFFFFF8D,
                    0xFFFFFF00,
                    0xFFFFEA00,
                    0xFFFFD600
            },
            new int[]{
                    0xFFFFE57F,
                    0xFFFFD740,
                    0xFFFFC400,
                    0xFFFFAB00
            },
            new int[]{
                    0xFFFFD180,
                    0xFFFFAB40,
                    0xFFFF9100,
                    0xFFFF6D00
            },
            new int[]{
                    0xFFFF9E80,
                    0xFFFF6E40,
                    0xFFFF3D00,
                    0xFFDD2C00
            }
    };

    private final static int[] ACCENT_COLORS_FREE = new int[]{
            0xFFFF1744,
//            0xFFF50057,
//            0xFFD500F9,
//            0xFF651FFF,
            0xFF3D5AFE,
//            0xFF2979FF,
            0xFF00B0FF,
//            0xFF00E5FF,
//            0xFF1DE9B6,
            0xFF00E676,
//            0xFF76FF03,
//            0xFFC6FF00,
//            0xFFFFEA00,
            0xFFFFC400,
//            0xFFFF9100,
            0xFFFF3D00
    };

    private final static int[][] ACCENT_COLORS_SUB_FREE = new int[][]{
            new int[]{
                    0xFFFF8A80,
                    0xFFFF5252,
                    0xFFFF1744,
                    0xFFD50000
            },
//            new int[]{
//                    0xFFFF80AB,
//                    0xFFFF4081,
//                    0xFFF50057,
//                    0xFFC51162
//            },
//            new int[]{
//                    0xFFEA80FC,
//                    0xFFE040FB,
//                    0xFFD500F9,
//                    0xFFAA00FF
//            },
//            new int[]{
//                    0xFFB388FF,
//                    0xFF7C4DFF,
//                    0xFF651FFF,
//                    0xFF6200EA
//            },
            new int[]{
                    0xFF8C9EFF,
                    0xFF536DFE,
                    0xFF3D5AFE,
                    0xFF304FFE
            },
//            new int[]{
//                    0xFF82B1FF,
//                    0xFF448AFF,
//                    0xFF2979FF,
//                    0xFF2962FF
//            },
            new int[]{
                    0xFF80D8FF,
                    0xFF40C4FF,
                    0xFF00B0FF,
                    0xFF0091EA
            },
//            new int[]{
//                    0xFF84FFFF,
//                    0xFF18FFFF,
//                    0xFF00E5FF,
//                    0xFF00B8D4
//            },
//            new int[]{
//                    0xFFA7FFEB,
//                    0xFF64FFDA,
//                    0xFF1DE9B6,
//                    0xFF00BFA5
//            },
            new int[]{
                    0xFFB9F6CA,
                    0xFF69F0AE,
                    0xFF00E676,
                    0xFF00C853
            },
//            new int[]{
//                    0xFFCCFF90,
//                    0xFFB2FF59,
//                    0xFF76FF03,
//                    0xFF64DD17
//            },
//            new int[]{
//                    0xFFF4FF81,
//                    0xFFEEFF41,
//                    0xFFC6FF00,
//                    0xFFAEEA00
//            },
//            new int[]{
//                    0xFFFFFF8D,
//                    0xFFFFFF00,
//                    0xFFFFEA00,
//                    0xFFFFD600
//            },
            new int[]{
                    0xFFFFE57F,
                    0xFFFFD740,
                    0xFFFFC400,
                    0xFFFFAB00
            },
//            new int[]{
//                    0xFFFFD180,
//                    0xFFFFAB40,
//                    0xFFFF9100,
//                    0xFFFF6D00
//            },
            new int[]{
                    0xFFFF9E80,
                    0xFFFF6E40,
                    0xFFFF3D00,
                    0xFFDD2C00
            }
    };
}