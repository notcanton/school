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
	// root_position = { 2.0f,1.0f,2.0f };
	root_position = { 2.0f,0.5f,2.0f };

	// target
	target = { 3.0f, 8.0f, 3.0f };

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

	// Compute joint locations
	joints[0] = glm::vec3({ 2.0f,1.0f,2.0f });
	for (int i = 1; i < scale_vector.size(); i++) {
		glm::vec4 joint = glm::vec4({ 0.0f,0.0f,0.0f, 1.0f });
		joint = transformNoScale(i) * joint;
		joints[i] = joint;
		//std::cout << glm::to_string(joint) << std::endl;
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
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z1), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y1), glm::vec3(0.0, 1.0, 0.0));
		}
		else if (i == 2) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X2), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z2), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y2), glm::vec3(0.0, 1.0, 0.0));
		}
		else if (i == 3) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X3), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z3), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y3), glm::vec3(0.0, 1.0, 0.0));
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

	//std::cout << glm::to_string(root_bone_obj_mat[0]) << std::endl;
	//std::cout << glm::to_string(root_bone_obj_mat[3]) << std::endl;

	return root_bone_obj_mat;
}

void Bone_Animation::computeJacobian() {
	glm::vec3 end_effector = joints[3];
	int col_index = 0;
	for (int i = 0; i < 3; i++) {
		glm::mat4 trans_mat = transformNoScale(i + 1);
		glm::vec3 joint = joints[i];
		for (int j = 0; j < 3; j++) {
			glm::vec3 col = glm::cross(glm::normalize(glm::vec3(trans_mat[j])), (end_effector - joint));
			//std::cout << "Joint: " << i  << std::endl;
			//std::cout << glm::to_string(joint) << std::endl;
			//std::cout << "trans: " << j << std::endl;
			//std::cout << glm::to_string(glm::vec3(trans_mat[j])) << std::endl;
			//std::cout << glm::to_string(col) << std::endl;
			jacobian.col(col_index) << col[0], col[1], col[2];
			col_index++;
		}
	}
}

glm::mat4 Bone_Animation::transformNoScale(int index) {
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
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z1), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y1), glm::vec3(0.0, 1.0, 0.0));
		}
		else if (i == 2) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X2), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z2), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y2), glm::vec3(0.0, 1.0, 0.0));
		}
		else if (i == 3) {
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(X3), glm::vec3(1.0, 0.0, 0.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Z3), glm::vec3(0.0, 0.0, 1.0));
			root_bone_obj_mat = glm::rotate(root_bone_obj_mat, glm::radians(Y3), glm::vec3(0.0, 1.0, 0.0));
		}
		else {
			std::cout << "ERROR: incorrect number of indices" << std::endl;
		}
		// Move bottom of box to next height in bone structure
		glm::vec3 curr_translate = glm::vec3({ 0.0f,0.0f,0.0f });
		curr_translate[1] += scale_vector[i][1];
		root_bone_obj_mat = glm::translate(root_bone_obj_mat, curr_translate);
	}

	//std::cout << glm::to_string(root_bone_obj_mat[0]) << std::endl;
	//std::cout << glm::to_string(root_bone_obj_mat[3]) << std::endl;

	return root_bone_obj_mat;
}

void Bone_Animation::update(float delta_time)
{
	if (bone_move && glm::distance(target, joints[3]) > 1e-6) {
		computeJacobian();
		float alpha = step_size();
		glm::vec3 diff = target - joints[3];
		Eigen::Vector3f v;
		v << diff[0], diff[1], diff[2];

		Eigen::VectorXf dof = (alpha * jacobian.transpose()) * v;
		
		//std::cout << "Dof" << std::endl;
		//std::cout << dof << std::endl;

		X1 += dof[0];
		Y1 += dof[1];
		Z1 += dof[2];
		X2 += dof[3];
		Y2 += dof[4];
		Z2 += dof[5];
		X3 += dof[6];
		Y3 += dof[7];
		Z3 += dof[8];

		for (int i = 1; i < scale_vector.size(); i++) {
			glm::vec4 joint = glm::vec4({ 0.0f,0.0f,0.0f, 1.0f });
			joint = transformNoScale(i) * joint;
			joints[i] = joint;
			//std::cout << glm::to_string(joint) << std::endl;
		}

		// std::cout << joints[3][0] << std::endl;
	}
	else {
		bone_move = false;
	}
}

float Bone_Animation::step_size() {
	glm::vec3 diff = target - joints[3];
	Eigen::Vector3f v;
	v << diff[0], diff[1], diff[2];
	auto temp = (jacobian.transpose() * v);
	float numerator = temp.norm();
	float denom = (jacobian * temp).norm();
	return numerator / denom;
}



void Bone_Animation::reset()
{
	X1 = 0.0f;
	Y1 = 0.0f;
	Z1 = 30.0f;
	X2 = 0.0f;
	Y2 = 0.0f;
	Z2 = 30.0f;
	X3 = 0.0f;
	Y3 = 0.0f;
	Z3 = 30.0f;
	bone_move = false;
	// Compute joint locations
	for (int i = 1; i < scale_vector.size(); i++) {
		glm::vec4 joint = glm::vec4({ 0.0f,0.0f,0.0f, 1.0f });
		joint = transformNoScale(i) * joint;
		joints[i] = joint;
		//std::cout << glm::to_string(joint) << std::endl;
	}
}

