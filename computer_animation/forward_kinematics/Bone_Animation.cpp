#include "Bone_Animation.h"
#include "glm/ext.hpp"


Bone_Animation::Bone_Animation()
{
}


Bone_Animation::~Bone_Animation()
{
}

void Bone_Animation::init()
{
	root_position = { 2.0f,1.0f,2.0f };

	scale_vector =
	{
		{1.0f,1.0f,1.0f},
		{0.5f,4.0f,0.5f},
		{0.5f,3.0f,0.5f},
		{0.5f,2.0f,0.5f}
	};

	rotation_degree_vector = 
	{
		{0.0f,0.0f,0.0f},
		{0.0f,0.0f,0.0f},
		{0.0f,0.0f,0.0f},
		{0.0f,0.0f,0.0f}
	};

	colors = 
	{
		{0.7f,0.0f,0.0f,1.0f},
		{0.7f,0.7f,0.0f,1.0f},
		{0.7f,0.0f,0.7f,1.0f},
		{0.0f,0.7f,0.7f,1.0f}
	};

	
	// Compute relative bone position
	glm::vec3 prev_carry = root_position;
	placement_vector.push_back(root_position);
	for (int i = 1; i < scale_vector.size(); i++) {
		glm::vec3 carry = glm::vec3({ 0.0f,0.0f,0.0f });
		carry += prev_carry;
		carry[1] += scale_vector[i - 1][1];
		prev_carry = carry;
		placement_vector.push_back(carry);
	}
}

glm::mat4 Bone_Animation::transform(int index) {
	glm::mat4 root_bone_obj_mat = glm::mat4(1.0f);


	// Move to bone placement
	glm::vec3 temp = glm::vec3({ 0.0f, -0.5f, 0.0f });
	temp += placement_vector[index];
	root_bone_obj_mat = glm::translate(root_bone_obj_mat, temp);

	// Move bone back to original placement after applying forward transfromations
	temp = glm::vec3({ 0.0f, 0.0f, 0.0f });
	for (int i = 1; i < index; i++) {
		temp[1] -= scale_vector[i][1];
	}
	root_bone_obj_mat = glm::translate(root_bone_obj_mat, temp);


	for (int i = 1; i <= index; i++) {
		// Rotate
		if (i == 1) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X1), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y1), glm::vec3(0.0, 1.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z1), glm::vec3(0.0, 0.0, 1.0));
		}
		else if (i == 2) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X2), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y2), glm::vec3(0.0, 1.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z2), glm::vec3(0.0, 0.0, 1.0));
		}
		else if (i == 3) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X3), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y3), glm::vec3(0.0, 1.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z3), glm::vec3(0.0, 0.0, 1.0));
		}
		else {
			std::cout << "ERROR: incorrect number of indices" << std::endl;
		}

		float height_scale = 0;
		if (index == i) {
			height_scale = 0.5f;
		}
		else {
			height_scale = 1.0f;
		}

		// Move bottom of box to next height in bone structure
		glm::vec3 curr_translate = glm::vec3({ 0.0f,0.0f,0.0f });
		curr_translate[1] += scale_vector[i][1] * height_scale;
		root_bone_obj_mat = glm::translate(root_bone_obj_mat, curr_translate);
	}

	// Scale
	root_bone_obj_mat = glm::scale(root_bone_obj_mat, scale_vector[index]);

	return root_bone_obj_mat;
}

void Bone_Animation::update(float delta_time)
{

}

void Bone_Animation::reset()
{
	X1 = 0.0f;
	Y1 = 0.0f;
	Z1 = 0.0f;
	X2 = 0.0f;
	Y2 = 0.0f;
	Z2 = 0.0f;
	X3 = 0.0f;
	Y3 = 0.0f;
	Z3 = 0.0f;
}

