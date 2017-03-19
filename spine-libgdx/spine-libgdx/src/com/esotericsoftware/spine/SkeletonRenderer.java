/******************************************************************************
 * Spine Runtimes Software License v2.5
 *
 * Copyright (c) 2013-2016, Esoteric Software
 * All rights reserved.
 *
 * You are granted a perpetual, non-exclusive, non-sublicensable, and
 * non-transferable license to use, install, execute, and perform the Spine
 * Runtimes software and derivative works solely for personal or internal
 * use. Without the written permission of Esoteric Software (see Section 2 of
 * the Spine Software License Agreement), you may not (a) modify, translate,
 * adapt, or develop new applications using the Spine Runtimes or otherwise
 * create derivative works or improvements of the Spine Runtimes or (b) remove,
 * delete, alter, or obscure any trademarks or any copyright, trademark, patent,
 * or other intellectual property or proprietary rights notices on or in the
 * Software, including any copy thereof. Redistributions in binary or source
 * form must include this license and terms.
 *
 * THIS SOFTWARE IS PROVIDED BY ESOTERIC SOFTWARE "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL ESOTERIC SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES, BUSINESS INTERRUPTION, OR LOSS OF
 * USE, DATA, OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.attachments.SkeletonAttachment;
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch;

public class SkeletonRenderer {
	static private final short[] quadTriangles = {0, 1, 2, 2, 3, 0};

	private boolean premultipliedAlpha;
	private final FloatArray vertices = new FloatArray(32);

	public void draw (Batch batch, Skeleton skeleton) {
		boolean premultipliedAlpha = this.premultipliedAlpha;
		float[] vertices = this.vertices.items;
		Color skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Array<Slot> drawOrder = skeleton.drawOrder;
		for (int i = 0, n = drawOrder.size; i < n; i++) {
			Slot slot = drawOrder.get(i);
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				region.computeWorldVertices(slot.getBone(), vertices, 0, 5);
				Color color = region.getColor(), slotColor = slot.getColor();
				float alpha = a * slotColor.a * color.a * 255;
				float c = NumberUtils.intToFloatColor(((int)alpha << 24) //
					| ((int)(b * slotColor.b * color.b * alpha) << 16) //
					| ((int)(g * slotColor.g * color.g * alpha) << 8) //
					| (int)(r * slotColor.r * color.r * alpha));
				float[] uvs = region.getUVs();
				for (int u = 0, v = 2; u < 8; u += 2, v += 5) {
					vertices[v] = c;
					vertices[v + 1] = uvs[u];
					vertices[v + 2] = uvs[u + 1];
				}

				BlendMode blendMode = slot.data.getBlendMode();
				batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
				batch.draw(region.getRegion().getTexture(), vertices, 0, 20);

			} else if (attachment instanceof MeshAttachment) {
				throw new RuntimeException("SkeletonMeshRenderer is required to render meshes.");

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton == null) continue;
				Bone bone = slot.getBone();
				Bone rootBone = attachmentSkeleton.getRootBone();
				float oldScaleX = rootBone.getScaleX();
				float oldScaleY = rootBone.getScaleY();
				float oldRotation = rootBone.getRotation();
				attachmentSkeleton.setPosition(bone.getWorldX(), bone.getWorldY());
				// rootBone.setScaleX(1 + bone.getWorldScaleX() - oldScaleX);
				// rootBone.setScaleY(1 + bone.getWorldScaleY() - oldScaleY);
				// Set shear.
				rootBone.setRotation(oldRotation + bone.getWorldRotationX());
				attachmentSkeleton.updateWorldTransform();

				draw(batch, attachmentSkeleton);

				attachmentSkeleton.setX(0);
				attachmentSkeleton.setY(0);
				rootBone.setScaleX(oldScaleX);
				rootBone.setScaleY(oldScaleY);
				rootBone.setRotation(oldRotation);
			}
		}
	}

	@SuppressWarnings("null")
	public void draw (PolygonSpriteBatch batch, Skeleton skeleton) {
		boolean premultipliedAlpha = this.premultipliedAlpha;
		BlendMode blendMode = null;
		int verticesLength = 0;
		float[] vertices = null, uvs = null;
		short[] triangles = null;
		Texture texture = null;
		Color color = null, skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Array<Slot> drawOrder = skeleton.drawOrder;
		for (int i = 0, n = drawOrder.size; i < n; i++) {
			Slot slot = drawOrder.get(i);
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				verticesLength = 20;
				vertices = this.vertices.items;
				region.computeWorldVertices(slot.getBone(), vertices, 0, 5);
				triangles = quadTriangles;
				texture = region.getRegion().getTexture();
				uvs = region.getUVs();
				color = region.getColor();

			} else if (attachment instanceof MeshAttachment) {
				MeshAttachment mesh = (MeshAttachment)attachment;
				int count = mesh.getWorldVerticesLength();
				verticesLength = (count >> 1) * 5;
				vertices = this.vertices.setSize(verticesLength);
				mesh.computeWorldVertices(slot, 0, count, vertices, 0, 5);
				triangles = mesh.getTriangles();
				texture = mesh.getRegion().getTexture();
				uvs = mesh.getUVs();
				color = mesh.getColor();

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton == null) continue;
				Bone bone = slot.getBone();
				Bone rootBone = attachmentSkeleton.getRootBone();
				float oldScaleX = rootBone.getScaleX();
				float oldScaleY = rootBone.getScaleY();
				float oldRotation = rootBone.getRotation();
				attachmentSkeleton.setPosition(bone.getWorldX(), bone.getWorldY());
				// rootBone.setScaleX(1 + bone.getWorldScaleX() - oldScaleX);
				// rootBone.setScaleY(1 + bone.getWorldScaleY() - oldScaleY);
				// Also set shear.
				rootBone.setRotation(oldRotation + bone.getWorldRotationX());
				attachmentSkeleton.updateWorldTransform();

				draw(batch, attachmentSkeleton);

				attachmentSkeleton.setPosition(0, 0);
				rootBone.setScaleX(oldScaleX);
				rootBone.setScaleY(oldScaleY);
				rootBone.setRotation(oldRotation);
				continue;
			}

			if (texture != null) {
				Color slotColor = slot.getColor();
				float alpha = a * slotColor.a * color.a * 255;
				float c = NumberUtils.intToFloatColor(((int)alpha << 24) //
					| ((int)(b * slotColor.b * color.b * alpha) << 16) //
					| ((int)(g * slotColor.g * color.g * alpha) << 8) //
					| (int)(r * slotColor.r * color.r * alpha));
				for (int v = 2, u = 0; v < verticesLength; v += 5, u += 2) {
					vertices[v] = c;
					vertices[v + 1] = uvs[u];
					vertices[v + 2] = uvs[u + 1];
				}

				BlendMode slotBlendMode = slot.data.getBlendMode();
				if (slotBlendMode != blendMode) {
					blendMode = slotBlendMode;
					batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
				}
				batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
			}
		}
	}

	@SuppressWarnings("null")
	public void draw (TwoColorPolygonBatch batch, Skeleton skeleton) {
		boolean premultipliedAlpha = this.premultipliedAlpha;
		BlendMode blendMode = null;
		int verticesLength = 0;
		float[] vertices = null, uvs = null;
		short[] triangles = null;
		Texture texture = null;
		Color color = null, skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Array<Slot> drawOrder = skeleton.drawOrder;
		for (int i = 0, n = drawOrder.size; i < n; i++) {
			Slot slot = drawOrder.get(i);
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				verticesLength = 24;
				vertices = this.vertices.items;
				region.computeWorldVertices(slot.getBone(), vertices, 0, 6);
				triangles = quadTriangles;
				texture = region.getRegion().getTexture();
				uvs = region.getUVs();
				color = region.getColor();

			} else if (attachment instanceof MeshAttachment) {
				MeshAttachment mesh = (MeshAttachment)attachment;
				int count = mesh.getWorldVerticesLength();
				verticesLength = count * 3;
				vertices = this.vertices.setSize(verticesLength);
				mesh.computeWorldVertices(slot, 0, count, vertices, 0, 6);
				triangles = mesh.getTriangles();
				texture = mesh.getRegion().getTexture();
				uvs = mesh.getUVs();
				color = mesh.getColor();

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton == null) continue;
				Bone bone = slot.getBone();
				Bone rootBone = attachmentSkeleton.getRootBone();
				float oldScaleX = rootBone.getScaleX();
				float oldScaleY = rootBone.getScaleY();
				float oldRotation = rootBone.getRotation();
				attachmentSkeleton.setPosition(bone.getWorldX(), bone.getWorldY());
				// rootBone.setScaleX(1 + bone.getWorldScaleX() - oldScaleX);
				// rootBone.setScaleY(1 + bone.getWorldScaleY() - oldScaleY);
				// Also set shear.
				rootBone.setRotation(oldRotation + bone.getWorldRotationX());
				attachmentSkeleton.updateWorldTransform();

				draw(batch, attachmentSkeleton);

				attachmentSkeleton.setPosition(0, 0);
				rootBone.setScaleX(oldScaleX);
				rootBone.setScaleY(oldScaleY);
				rootBone.setRotation(oldRotation);
				continue;
			}

			if (texture != null) {
				Color lightColor = slot.getColor();
				float alpha = a * lightColor.a * color.a * 255;
				float light = NumberUtils.intToFloatColor(((int)alpha << 24) //
					| ((int)(b * lightColor.b * color.b * alpha) << 16) //
					| ((int)(g * lightColor.g * color.g * alpha) << 8) //
					| (int)(r * lightColor.r * color.r * alpha));
				Color darkColor = slot.getDarkColor();
				if (darkColor == null) darkColor = Color.BLACK;
				float dark = NumberUtils.intToFloatColor( //
					((int)(b * darkColor.b * color.b * 255) << 16) //
						| ((int)(g * darkColor.g * color.g * 255) << 8) //
						| (int)(r * darkColor.r * color.r * 255));
				for (int v = 2, u = 0; v < verticesLength; v += 6, u += 2) {
					vertices[v] = light;
					vertices[v + 1] = dark;
					vertices[v + 2] = uvs[u];
					vertices[v + 3] = uvs[u + 1];
				}

				BlendMode slotBlendMode = slot.data.getBlendMode();
				if (slotBlendMode != blendMode) {
					blendMode = slotBlendMode;
					batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
				}
				batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
			}
		}
	}

	public void setPremultipliedAlpha (boolean premultipliedAlpha) {
		this.premultipliedAlpha = premultipliedAlpha;
	}
}
