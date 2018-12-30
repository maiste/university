package view.graphic;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

/**
 * Cette classe permet de crée un objet qui lira le fichier audio qui lui est passer en paramètre
 * lors de la construction de celui-ci, deux modes de lecture sont disponible (une fois ou en boucle)
 */

public class Audio {
	
	private Clip music;

    /**
     * @param filePath chemin du fichier audio a lire
     */
	public Audio (String filePath) {
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
			AudioFormat format = ais.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			music = (Clip)AudioSystem.getLine(info);
			music.open(ais);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * joue l'audio une seule fois
     */
	public void play() {
		music.setFramePosition(0);
		music.start();
	}

    /**
     * boucle l'audio
     */
	public void loop() {
		music.loop(Clip.LOOP_CONTINUOUSLY);
	}

    /**
     * stop l'audio
      */
	public void stop() {
		music.stop();
	}
}
