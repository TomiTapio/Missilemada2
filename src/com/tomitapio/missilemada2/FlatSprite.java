package com.tomitapio.missilemada2;
import java.util.Vector;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

public class FlatSprite extends MobileThing {
  float width, height, texturescaling, tr;
  float perm_rotation = 0.0f;

  public FlatSprite(double in_width, double in_height, double x, double y, double z, String texturefilename, double in_texturescaling, float transp) {
    xcoord = x;
    ycoord = y;
    zcoord = z;
    xspeed = 0.0;
    yspeed = 0.0;
    zspeed = 0.0;

    width = (float)in_width;
    height = (float)in_height;
    mass_kg = 0.0;
    unique_id = MobileThing.getNextId();

    //xxx verify inputs

    spotted_by_player = true; //initially visible.

    corenote = 90; // MIDI note 0..127
    MIDI_instrument = 1; //default acoustic piano

    setTexture(texturefilename, 1.0);
    texturescaling = (float)in_texturescaling;
    tr = transp;
  }
  public void setPermRotation(float a) {
    perm_rotation = a;
  }
  public void advance_time (double seconds) {
    int wti = com.tomitapio.missilemada2.Missilemada2.getWorldTimeIncrement();
    if (wti < 1) return; //zero means pause

    //move
    xcoord = xcoord + (xspeed * seconds);
    ycoord = ycoord + (yspeed * seconds);
    zcoord = zcoord + (zspeed * seconds);
  }
  public void drawFlatSprite(float scale1, float rotation_deg, boolean correctionflip) { //preferably has much alpha channel, to appear non-square.
    //GL11.glEnable(GL_DEPTH_TEST);
    if (textureMy != null) {
      textureMy.bind();
    }
    GL11.glPushMatrix();
    GL11.glColor4f(1f, 1f, 1f, tr);
    GL11.glTranslatef((float)xcoord, (float)ycoord, (float)zcoord);

    GL11.glRotatef(180, 1f,0f,0f); //x 180deg makes correct side up. and textured side towards camera.

    GL11.glScalef(scale1, scale1, scale1);
    GL11.glRotatef(rotation_deg, 0f, 0f, 1f); //rotate on z-axis so x and y change.
    if (perm_rotation > 0.01f) //for clouds
      GL11.glRotatef(perm_rotation, 0f, 0f, 1f);

    GL11.glBegin(GL11.GL_QUADS);
    if (textureMy != null)
      GL11.glTexCoord2f(0, 0);
      GL11.glVertex3f(0, 0, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    if (textureMy != null)
      GL11.glTexCoord2f(0, texturescaling * textureMy.getHeight());
      GL11.glVertex3f(0, height, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    if (textureMy != null)
      GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), texturescaling * textureMy.getHeight());
      GL11.glVertex3f(width, height, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    if (textureMy != null)
      GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), 0);
      GL11.glVertex3f(width, 0, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glEnd();

    GL11.glPopMatrix();
  }
  public void drawFlatSpriteSHIP(float scale1, float rotation_deg) {
    //color and transparency set by caller. push&pop handling by caller.
    if (textureMy != null)
      textureMy.bind();
    GL11.glPushMatrix(); //needed coz scale command hurts ships-camera spheres.
    GL11.glTranslatef((float)xcoord, (float)ycoord, (float)zcoord);
    GL11.glScalef(scale1, scale1, 1.0f);
    GL11.glRotatef(180, 1f,0f,0f); //180deg makes correct side up. and textured side towards camera.
    GL11.glRotatef(rotation_deg, 0f, 0f, 1f); //rotate on z-axis so x and y change.

    GL11.glBegin(GL11.GL_QUADS);

      GL11.glTexCoord2f(0, 0);
    GL11.glVertex3f(-width/2.0f, -height/2.0f, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);

      GL11.glTexCoord2f(0, texturescaling * textureMy.getHeight());
    GL11.glVertex3f(-width/2.0f, height/2.0f, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);

      GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), texturescaling * textureMy.getHeight());
    GL11.glVertex3f(width/2.0f, height/2.0f, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);

      GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), 0);
    GL11.glVertex3f(width/2.0f, -height/2.0f, 0);      GL11.glNormal3f(0.0f, 0.0f, 1.0f);

    GL11.glEnd();

    GL11.glPopMatrix();
  }
  public static void drawFlatLine(double x1, double y1, double z1, double x2, double y2, double z2, double width) { //xxx knows which way to face camera?
    GL11.glEnable(GL_DEPTH_TEST);
    //GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex3f((float) x1, (float) y1, (float) z1);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
    GL11.glVertex3f((float) (x1 + width), (float) (y1 + width), (float) (z1+width)       );      GL11.glNormal3f(0.0f, 0.0f, 1.0f);

    GL11.glVertex3f((float) (x2 + width), (float) (y2 + width), (float) (z2 + width));   GL11.glNormal3f(0.0f, 0.0f, 1.0f);

    GL11.glVertex3f((float) x2, (float) y2, (float) z2);   GL11.glNormal3f(0.0f, 0.0f, 1.0f);

    GL11.glEnd();
  }
  public static void drawFlatLineVecVec(Vector xyz1, Vector xyz2, double width) {
    if (xyz1 == null || xyz2 == null)
      return;
    drawFlatLine(((Double) xyz1.get(0)), ((Double) xyz1.get(1)), ((Double) xyz1.get(2)),
                 ((Double) xyz2.get(0)), ((Double) xyz2.get(1)), ((Double) xyz2.get(2)), width);
  }
  public void drawBox() {
    // this func draws a box with some texture coordinates
    glBegin(GL_QUADS);
    // Front Face
    glTexCoord2f(0.0f, 0.0f); glVertex3f(-1.0f, -1.0f,  1.0f);	// Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f); glVertex3f( 1.0f, -1.0f,  1.0f);	// Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f); glVertex3f( 1.0f,  1.0f,  1.0f);	// Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f); glVertex3f(-1.0f,  1.0f,  1.0f);	// Top Left Of The Texture and Quad
    // Back Face
    glTexCoord2f(1.0f, 0.0f); glVertex3f(-1.0f, -1.0f, -1.0f);	// Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f); glVertex3f(-1.0f,  1.0f, -1.0f);	// Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f); glVertex3f( 1.0f,  1.0f, -1.0f);	// Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f); glVertex3f( 1.0f, -1.0f, -1.0f);	// Bottom Left Of The Texture and Quad
    // Top Face
    glTexCoord2f(0.0f, 1.0f); glVertex3f(-1.0f,  1.0f, -1.0f);	// Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f); glVertex3f(-1.0f,  1.0f,  1.0f);	// Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f); glVertex3f( 1.0f,  1.0f,  1.0f);	// Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f); glVertex3f( 1.0f,  1.0f, -1.0f);	// Top Right Of The Texture and Quad
    // Bottom Face
    glTexCoord2f(1.0f, 1.0f); glVertex3f(-1.0f, -1.0f, -1.0f);	// Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f); glVertex3f( 1.0f, -1.0f, -1.0f);	// Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f); glVertex3f( 1.0f, -1.0f,  1.0f);	// Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f); glVertex3f(-1.0f, -1.0f,  1.0f);	// Bottom Right Of The Texture and Quad
    // Right face
    glTexCoord2f(1.0f, 0.0f); glVertex3f( 1.0f, -1.0f, -1.0f);	// Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f); glVertex3f( 1.0f,  1.0f, -1.0f);	// Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f); glVertex3f( 1.0f,  1.0f,  1.0f);	// Top Left Of The Texture and Quad
    glTexCoord2f(0.0f, 0.0f); glVertex3f( 1.0f, -1.0f,  1.0f);	// Bottom Left Of The Texture and Quad
    // Left Face
    glTexCoord2f(0.0f, 0.0f); glVertex3f(-1.0f, -1.0f, -1.0f);	// Bottom Left Of The Texture and Quad
    glTexCoord2f(1.0f, 0.0f); glVertex3f(-1.0f, -1.0f,  1.0f);	// Bottom Right Of The Texture and Quad
    glTexCoord2f(1.0f, 1.0f); glVertex3f(-1.0f,  1.0f,  1.0f);	// Top Right Of The Texture and Quad
    glTexCoord2f(0.0f, 1.0f); glVertex3f(-1.0f,  1.0f, -1.0f);	// Top Left Of The Texture and Quad
    glEnd();
  }
}
