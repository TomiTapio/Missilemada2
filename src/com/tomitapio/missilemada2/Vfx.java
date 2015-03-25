package com.tomitapio.missilemada2;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;

import java.io.IOException;
import java.util.Vector;

/* creation date: 15.6.2012 for TomiTapio's Missilemada 1 */
public class Vfx { //similar to FlatSprite but mostly doesn't move, and expires at designated time.
  double xcoord, ycoord, zcoord;
  float width, height, texturescaling;
  TextureMy textureMy = null;
  boolean hastexture = false;
  float sizeness;
  float trans = 1.0f;
  long expiration_time; //in worldtime milliseconds when to delete.
  String effect;
  MobileThing attachedTo = null;
  String text = "";
  double host_x = 0;
  double host_y = 0;
  double host_z = 0;
  double xoff = 0;
  double yoff = 0;
  double zoff = 0;


  int host_pixelradius = 2;

  public Vfx (double x, double y, double z, String in_fx, int duration/*seconds*/, double si, double tr, MobileThing m,
              String texture_filename, double in_texturescaling, String in_text) {
    expiration_time = com.tomitapio.missilemada2.Missilemada2.getWorldTime() + duration;
    xcoord = x;
    ycoord = y;
    zcoord = z;
    effect = in_fx;
    text = in_text;
    sizeness = (float)si;
    trans = (float)tr;
    attachedTo = m;
    if (attachedTo != null) { //then xyz are relative to mobilething.
      xoff = x;
      yoff = y;
      zoff = z;
    }
    try {
      if (texture_filename.length() > 0) {
        textureMy = com.tomitapio.missilemada2.Missilemada2.getTextureLoader().getTexture(texture_filename);
        texturescaling = (float)in_texturescaling;
        hastexture = true;
      } else {
        hastexture = false;
      }
    } catch (IOException e) {
      System.out.println("Vfx: Unable to load texture file: "+texture_filename);
      //e.printStackTrace();
    }
  }
  public long getExpirationTime() {
    return expiration_time;
  }
  public boolean isNearCamera(Vector cam_xyz, double cam_z) {
    //only look at x and y.
    if (MobileThing.calcDistance(xcoord, ycoord, 0.0, ((Double) cam_xyz.get(0)).doubleValue(), ((Double) cam_xyz.get(1)).doubleValue(), 0.0) < cam_z)
      return true;
    else
      return false;
  }
  public void drawVfx(float sca, long wtime) {
    if (attachedTo != null) {
      host_x = attachedTo.getX();
      host_y = attachedTo.getY();
      host_z = attachedTo.getZ();
      //set Vfx's coords to MobileThing's current location, plus creation-given offsets.
      xcoord = host_x + xoff;
      ycoord = host_y + yoff;
      zcoord = host_z + zoff;
      host_pixelradius = attachedTo.getPixelRadius(); // defaults to 2

      //if not seen by player, don't draw (ship-attached) vfx.
      if (!attachedTo.isSeenByPlayer()) {
        return;
      }
    }

    if (true) { //effect.equals("sumthin")
      width = sizeness * 2.5f;
      height = sizeness * 2.5f;
      //moved material and enabletexture outside drawVfx!
      GL11.glPushMatrix();

      //text vfx is mostly for debug. No need for translate, only scale.
      if (effect.equals("TEXT")) {
        if (text.length() > 0) {
          //System.out.println("vfx: drawing text vfx of "+text);
          //Missilemada2.setOpenGLMaterial("FONT");
          Missilemada2.drawTextXYZ(Missilemada2.getFont60fornowwww(0), 500f, (float) xcoord, (float) (ycoord - height), (float) zcoord, text, Color.gray);
        }

      } else { //all regular vfx, a quad with texture.

      if (hastexture) {
        if (textureMy != null)
          textureMy.bind();
      }

      GL11.glColor4f(0.9f, 0.9f, 0.9f, trans);
      GL11.glTranslatef((float)(xcoord), (float)(ycoord), (float)zcoord); //the core, regular translate.
      GL11.glScalef(sca, sca, sca);
      if (effect.equals("SCOUTEDASTEROID")) { //then some animation.
        float scann = (float) (0.01*((wtime / 360.0) % 700.0));
        GL11.glScalef(scann, scann, 1.0f);
      }

        GL11.glRotatef(180.0f, 1.0f, 0.0f, 0.0f); //rotate to fix upside-down texture image.
        GL11.glTranslatef(-(float)(width/2.0), -(float)(height/2.0), 0.0f);

        GL11.glBegin(GL11.GL_QUADS);
          if (hastexture)
            GL11.glTexCoord2f(0, 0);
          GL11.glVertex3f(0, 0, 0);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
          if (hastexture)
            GL11.glTexCoord2f(0, texturescaling * textureMy.getHeight());
          GL11.glVertex3f(0, height, 0);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
          if (hastexture)
            GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), texturescaling * textureMy.getHeight());
          GL11.glVertex3f(width, height, 0);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
          if (hastexture)
            GL11.glTexCoord2f(texturescaling * textureMy.getWidth(), 0);
          GL11.glVertex3f(width, 0, 0);          GL11.glNormal3f(0.0f, 0.0f, 1.0f);
        GL11.glEnd();



      //if (hastexture)
           ///slow        GL11.glDisable(GL11.GL_TEXTURE_2D);
      }
      GL11.glPopMatrix();
    } else {
      System.out.println("Vfx: unknown draw request "+effect);
    }
  }
}