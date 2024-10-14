package com.pasc.lib.barcodescanner;

import com.pasc.lib.zxing.BarcodeFormat;
import com.pasc.lib.zxing.DecodeHintType;
import com.pasc.lib.zxing.MultiFormatReader;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * DecoderFactory that creates a MultiFormatReader with specified hints.
 */
public class DefaultDecoderFactory implements DecoderFactory {
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> hints;
    private String characterSet;

    public DefaultDecoderFactory() {
    }

    public DefaultDecoderFactory(Collection<BarcodeFormat> decodeFormats, Map<DecodeHintType, ?> hints, String characterSet) {
        this.decodeFormats = decodeFormats;
        this.hints = hints;
        this.characterSet = characterSet;
    }

    @Override
    public Decoder createDecoder(Map<DecodeHintType, ?> baseHints) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

        hints.putAll(baseHints);

        if(this.hints != null) {
            hints.putAll(this.hints);
        }

        if(this.decodeFormats != null) {
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        }

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

        return new Decoder(reader);
    }
}
